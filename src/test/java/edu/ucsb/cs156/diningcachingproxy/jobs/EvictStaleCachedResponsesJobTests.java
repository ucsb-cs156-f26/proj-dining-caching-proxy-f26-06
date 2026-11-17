package edu.ucsb.cs156.diningcachingproxy.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class EvictStaleCachedResponsesJobTests {

  @Test
  public void deletes_entries_not_accessed_since_the_cutoff_and_logs_the_count() throws Exception {
    CachedResponseRepository repository = mock(CachedResponseRepository.class);
    Instant cutoff = Instant.parse("2026-10-16T12:00:00Z");
    when(repository.deleteNotAccessedSince(cutoff)).thenReturn(42L);
    EvictStaleCachedResponsesJob job =
        EvictStaleCachedResponsesJob.builder()
            .cachedResponseRepository(repository)
            .cutoff(cutoff)
            .build();
    JobContext ctx = new JobContext(null, Job.builder().build());

    job.accept(ctx);

    verify(repository).deleteNotAccessedSince(cutoff);
    assertEquals(
        "Evicting cached responses not accessed since 2026-10-16T12:00:00Z\n"
            + "Evicted 42 cached response(s)",
        ctx.getJob().getLog());
  }

  @Test
  public void job_name_is_the_class_name() {
    assertEquals(
        "EvictStaleCachedResponsesJob",
        EvictStaleCachedResponsesJob.builder().build().getJobName());
  }
}
