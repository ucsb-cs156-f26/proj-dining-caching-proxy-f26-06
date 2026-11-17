package edu.ucsb.cs156.diningcachingproxy.jobs;

import edu.ucsb.cs156.diningcachingproxy.services.EvictStaleCachedResponsesJobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Launches a cache eviction run on a schedule (nightly at 3am Pacific by default; override with
 * {@code app.cache.eviction.cron}, or set it to {@code -} to disable). Scheduling itself is enabled
 * by lib-jobs' auto-configuration.
 */
@Component
public class CacheEvictionScheduler {

  @Autowired private EvictStaleCachedResponsesJobFactory evictStaleCachedResponsesJobFactory;

  @Scheduled(cron = "${app.cache.eviction.cron:0 0 3 * * *}", zone = "America/Los_Angeles")
  public void evictStaleCachedResponses() {
    evictStaleCachedResponsesJobFactory.launch();
  }
}
