package edu.ucsb.cs156.diningcachingproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponse;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.diningcachingproxy.jobs.CacheEvictionScheduler;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end verification of cache eviction against a real (embedded) MongoDB: the repository's
 * delete query keeps recently-accessed entries and removes stale and pre-existing (no timestamp)
 * ones, and a scheduled run — which has no request and therefore no security context — completes as
 * a lib-jobs job with an accurate log.
 */
@SpringBootTest
@ActiveProfiles("integration")
public class CacheEvictionIntegrationTests {

  @Autowired private CachedResponseRepository cachedResponseRepository;
  @Autowired private CacheEvictionScheduler cacheEvictionScheduler;
  @Autowired private JobsRepository jobsRepository;
  @Autowired private JobService jobService;

  private final Instant now = Instant.now();

  @BeforeEach
  public void seed() {
    cachedResponseRepository.deleteAll();
    cachedResponseRepository.saveAll(
        List.of(
            entry("/dining/commons/v1/", now.minus(Duration.ofDays(1))),
            entry("/dining/menu/v1/2026-08-23/carrillo", now.minus(Duration.ofDays(40))),
            entry("/dining/menu/v1/2024-08-16/ortega", null)));
  }

  private CachedResponse entry(String path, Instant lastAccessedAt) {
    return CachedResponse.builder()
        .requestPath(path)
        .responseStatus(200)
        .responseBody("[]")
        .hitCount(1)
        .lastAccessedAt(lastAccessedAt)
        .build();
  }

  private List<String> remainingPaths() {
    return cachedResponseRepository.findAll().stream().map(CachedResponse::getRequestPath).toList();
  }

  @Test
  public void delete_query_removes_stale_and_untimestamped_entries_and_keeps_fresh_ones() {
    long deleted = cachedResponseRepository.deleteNotAccessedSince(now.minus(Duration.ofDays(30)));

    assertEquals(2, deleted);
    assertEquals(List.of("/dining/commons/v1/"), remainingPaths());
  }

  @Test
  public void scheduled_run_with_no_security_context_evicts_stale_entries_as_a_logged_job()
      throws Exception {
    jobsRepository.deleteAll();
    SecurityContextHolder.clearContext(); // what a @Scheduled thread sees

    cacheEvictionScheduler.evictStaleCachedResponses();

    Job job = awaitSingleFinishedJob();
    assertEquals("complete", job.getStatus());
    assertEquals("EvictStaleCachedResponsesJob", job.getJobName());
    String log = jobService.getJobLogs(job.getId());
    assertTrue(log.contains("Evicted 2 cached response(s)"), log);
    assertEquals(List.of("/dining/commons/v1/"), remainingPaths());
  }

  private Job awaitSingleFinishedJob() throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(10);
    while (Instant.now().isBefore(deadline)) {
      List<Job> jobs = jobsRepository.findAll();
      if (jobs.size() == 1 && List.of("complete", "error").contains(jobs.get(0).getStatus())) {
        return jobs.get(0);
      }
      Thread.sleep(50);
    }
    throw new AssertionError("eviction job did not finish within 10 seconds");
  }
}
