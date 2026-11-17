package edu.ucsb.cs156.diningcachingproxy.services;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.diningcachingproxy.jobs.EvictStaleCachedResponsesJob;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds and launches an {@link EvictStaleCachedResponsesJob}. Shared by the nightly scheduled run
 * and the admin's manual trigger, so there's exactly one place that turns "max age in days" into a
 * cutoff instant.
 */
@Service
public class EvictStaleCachedResponsesJobFactory {

  @Autowired private CachedResponseRepository cachedResponseRepository;
  @Autowired private JobService jobService;
  @Autowired private Clock clock;

  @Value("${app.cache.eviction.max-age-days:30}")
  private int defaultMaxAgeDays;

  /** Launches an eviction run using the configured default max age. */
  public Job launch() {
    return launch(defaultMaxAgeDays);
  }

  /**
   * Launches an eviction run that deletes every entry not accessed in the last {@code maxAgeDays}
   * days. Zero is allowed and evicts everything not accessed as of right now.
   */
  public Job launch(int maxAgeDays) {
    if (maxAgeDays < 0) {
      throw new IllegalArgumentException("maxAgeDays must be >= 0, was " + maxAgeDays);
    }
    Instant cutoff = clock.instant().minus(Duration.ofDays(maxAgeDays));
    EvictStaleCachedResponsesJob job =
        EvictStaleCachedResponsesJob.builder()
            .cachedResponseRepository(cachedResponseRepository)
            .cutoff(cutoff)
            .build();
    return jobService.runAsJob(job);
  }
}
