package edu.ucsb.cs156.diningcachingproxy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.diningcachingproxy.ControllerTestCase;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponse;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.diningcachingproxy.services.EvictStaleCachedResponsesJobFactory;
import edu.ucsb.cs156.jobs.entities.Job;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CachedEndpointsController.class)
public class CachedEndpointsControllerTests extends ControllerTestCase {

  @MockitoBean CachedResponseRepository cachedResponseRepository;
  @MockitoBean EvictStaleCachedResponsesJobFactory evictStaleCachedResponsesJobFactory;

  @Test
  public void logged_out_users_cannot_get_endpoints() throws Exception {
    mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_endpoints() throws Exception {
    mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_list_cached_endpoints_with_hit_counts_and_last_access() throws Exception {
    Instant commonsAccessed = Instant.parse("2026-11-14T08:30:00Z");
    CachedResponse commons =
        CachedResponse.builder()
            .requestPath("/dining/commons/v1/")
            .responseStatus(200)
            .responseBody("[]")
            .hitCount(5)
            .lastAccessedAt(commonsAccessed)
            .build();
    CachedResponse menu =
        CachedResponse.builder()
            .requestPath("/dining/menu/v1/2024-08-16/carrillo")
            .responseStatus(200)
            .responseBody("{}")
            .hitCount(1)
            .build();
    when(cachedResponseRepository.findAll()).thenReturn(Arrays.asList(commons, menu));

    MvcResult response =
        mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().isOk()).andReturn();

    List<CachedEndpointsController.CachedEndpointDTO> expected =
        List.of(
            new CachedEndpointsController.CachedEndpointDTO(
                "/dining/commons/v1/", 5, commonsAccessed),
            // cached before lastAccessedAt existed
            new CachedEndpointsController.CachedEndpointDTO(
                "/dining/menu/v1/2024-08-16/carrillo", 1, null));
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @Test
  public void logged_out_users_cannot_trigger_eviction() throws Exception {
    mockMvc.perform(post("/api/admin/endpoints/evict").with(csrf())).andExpect(status().is(403));
    verifyNoInteractions(evictStaleCachedResponsesJobFactory);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_trigger_eviction() throws Exception {
    mockMvc.perform(post("/api/admin/endpoints/evict").with(csrf())).andExpect(status().is(403));
    verifyNoInteractions(evictStaleCachedResponsesJobFactory);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_trigger_eviction_with_the_default_max_age() throws Exception {
    Job job = Job.builder().id(7).jobName("EvictStaleCachedResponsesJob").status("queued").build();
    when(evictStaleCachedResponsesJobFactory.launch()).thenReturn(job);

    MvcResult response =
        mockMvc
            .perform(post("/api/admin/endpoints/evict").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(evictStaleCachedResponsesJobFactory).launch();
    assertEquals(mapper.writeValueAsString(job), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_trigger_eviction_with_an_explicit_max_age() throws Exception {
    Job job = Job.builder().id(8).jobName("EvictStaleCachedResponsesJob").status("queued").build();
    when(evictStaleCachedResponsesJobFactory.launch(0)).thenReturn(job);

    MvcResult response =
        mockMvc
            .perform(post("/api/admin/endpoints/evict?maxAgeDays=0").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(evictStaleCachedResponsesJobFactory).launch(0);
    assertEquals(mapper.writeValueAsString(job), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void a_negative_max_age_is_a_bad_request() throws Exception {
    when(evictStaleCachedResponsesJobFactory.launch(-1))
        .thenThrow(new IllegalArgumentException("maxAgeDays must be >= 0, was -1"));

    MvcResult response =
        mockMvc
            .perform(post("/api/admin/endpoints/evict?maxAgeDays=-1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of("type", "IllegalArgumentException", "message", "maxAgeDays must be >= 0, was -1"),
        responseToJson(response));
  }
}
