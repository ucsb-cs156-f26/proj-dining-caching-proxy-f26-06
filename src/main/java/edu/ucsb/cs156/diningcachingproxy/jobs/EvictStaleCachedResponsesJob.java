package edu.ucsb.cs156.diningcachingproxy.jobs;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.time.Instant;
import lombok.Builder;

/**
 * Deletes every cached response not accessed since {@code cutoff}. Many upstream paths are
 * date-parameterized (e.g. {@code /dining/menu/v1/2026-08-23/carrillo}) and are almost never
 * requested again once their date passes, so without this the cache collection would grow without
 * bound. Run via {@code JobService} so each run (and how much it deleted) is visible at {@code
 * /api/jobs}.
 */
@Builder
public class EvictStaleCachedResponsesJob implements JobContextConsumer {

  private CachedResponseRepository cachedResponseRepository;
  private Instant cutoff;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Evicting cached responses not accessed since " + cutoff);
    long deleted = cachedResponseRepository.deleteNotAccessedSince(cutoff);
    ctx.log("Evicted " + deleted + " cached response(s)");
  }
}
