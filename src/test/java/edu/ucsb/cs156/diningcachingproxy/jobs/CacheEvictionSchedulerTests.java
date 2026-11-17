package edu.ucsb.cs156.diningcachingproxy.jobs;

import static org.mockito.Mockito.verify;

import edu.ucsb.cs156.diningcachingproxy.services.EvictStaleCachedResponsesJobFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheEvictionSchedulerTests {

  @Mock EvictStaleCachedResponsesJobFactory evictStaleCachedResponsesJobFactory;

  @InjectMocks CacheEvictionScheduler scheduler;

  @Test
  public void scheduled_run_launches_eviction_with_the_default_max_age() {
    scheduler.evictStaleCachedResponses();

    verify(evictStaleCachedResponsesJobFactory).launch();
  }
}
