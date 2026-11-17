package edu.ucsb.cs156.diningcachingproxy.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.diningcachingproxy.jobs.EvictStaleCachedResponsesJob;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

public class EvictStaleCachedResponsesJobFactoryTests {

  private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

  private CachedResponseRepository cachedResponseRepository;
  private JobService jobService;
  private EvictStaleCachedResponsesJobFactory factory;
  private final Job expectedJob = Job.builder().build();

  @BeforeEach
  public void setup() {
    cachedResponseRepository = mock(CachedResponseRepository.class);
    jobService = mock(JobService.class);
    when(jobService.runAsJob(any(EvictStaleCachedResponsesJob.class))).thenReturn(expectedJob);

    factory = new EvictStaleCachedResponsesJobFactory();
    ReflectionTestUtils.setField(factory, "cachedResponseRepository", cachedResponseRepository);
    ReflectionTestUtils.setField(factory, "jobService", jobService);
    ReflectionTestUtils.setField(factory, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
    ReflectionTestUtils.setField(factory, "defaultMaxAgeDays", 30);
  }

  /** Runs the job that was handed to JobService and returns the cutoff it passed to the repo. */
  private Instant cutoffUsedByLaunchedJob() throws Exception {
    ArgumentCaptor<EvictStaleCachedResponsesJob> captor =
        ArgumentCaptor.forClass(EvictStaleCachedResponsesJob.class);
    verify(jobService).runAsJob(captor.capture());
    captor.getValue().accept(new JobContext(null, Job.builder().build()));
    ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
    verify(cachedResponseRepository).deleteNotAccessedSince(cutoff.capture());
    return cutoff.getValue();
  }

  @Test
  public void launch_with_no_argument_uses_the_configured_default_max_age() throws Exception {
    Job result = factory.launch();

    assertSame(expectedJob, result);
    assertEquals(Instant.parse("2026-10-16T12:00:00Z"), cutoffUsedByLaunchedJob());
  }

  @Test
  public void launch_with_an_explicit_max_age_computes_the_cutoff_from_it() throws Exception {
    Job result = factory.launch(7);

    assertSame(expectedJob, result);
    assertEquals(Instant.parse("2026-11-08T12:00:00Z"), cutoffUsedByLaunchedJob());
  }

  @Test
  public void launch_with_zero_max_age_uses_now_as_the_cutoff() throws Exception {
    factory.launch(0);

    assertEquals(NOW, cutoffUsedByLaunchedJob());
  }

  @Test
  public void launch_with_a_negative_max_age_throws_and_launches_nothing() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> factory.launch(-1));

    assertEquals("maxAgeDays must be >= 0, was -1", e.getMessage());
    verifyNoInteractions(jobService);
  }
}
