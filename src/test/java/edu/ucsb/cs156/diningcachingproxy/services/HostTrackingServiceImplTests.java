package edu.ucsb.cs156.diningcachingproxy.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class HostTrackingServiceImplTests {

  @Test
  public void starts_with_no_hosts() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();
    assertEquals(List.of(), service.getHostCounts());
  }

  @Test
  public void recordRequest_returns_true_only_the_first_time_an_address_is_seen() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    assertTrue(service.recordRequest("10.0.0.5", "/dining/commons/v1/"));
    assertFalse(service.recordRequest("10.0.0.5", "/dining/commons/v1/"));
    assertFalse(service.recordRequest("10.0.0.5", "/dining/commons/v1/"));

    List<HostCount> counts = service.getHostCounts();
    assertEquals(1, counts.size());
    assertEquals("10.0.0.5", counts.get(0).host());
    assertEquals(3, counts.get(0).count());
  }

  @Test
  public void a_new_address_displays_as_itself_until_resolved() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("10.0.0.5", "/dining/commons/v1/");

    List<HostCount> counts = service.getHostCounts();
    assertEquals(1, counts.size());
    assertEquals("10.0.0.5", counts.get(0).host());
  }

  @Test
  public void resolveHostname_updates_the_display_name_without_losing_the_count() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("10.0.0.5", "/dining/commons/v1/");
    service.recordRequest("10.0.0.5", "/dining/commons/v1/");
    service.resolveHostname("10.0.0.5", "dining-qa.dokku-00.cs.ucsb.edu");
    service.recordRequest("10.0.0.5", "/dining/commons/v1/");

    List<HostCount> counts = service.getHostCounts();
    assertEquals(1, counts.size());
    assertEquals("dining-qa.dokku-00.cs.ucsb.edu", counts.get(0).host());
    assertEquals(3, counts.get(0).count());
  }

  @Test
  public void counts_are_sorted_by_host() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("zzz.example.edu", "/dining/commons/v1/");
    service.recordRequest("aaa.example.edu", "/dining/commons/v1/");
    service.recordRequest("mmm.example.edu", "/dining/commons/v1/");

    List<String> hosts = service.getHostCounts().stream().map(HostCount::host).toList();
    assertEquals(List.of("aaa.example.edu", "mmm.example.edu", "zzz.example.edu"), hosts);
  }

  @Test
  public void tracks_counts_per_path_for_a_host() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("10.0.0.5", "/dining/commons/v1/");
    service.recordRequest("10.0.0.5", "/dining/commons/v1/");
    service.recordRequest("10.0.0.5", "/dining/menu/v1/2024-08-16/carrillo");

    List<HostCount> counts = service.getHostCounts();
    assertEquals(1, counts.size());
    assertEquals(3, counts.get(0).count());

    List<PathCount> pathCounts = counts.get(0).pathCounts();
    assertEquals(
        List.of(
            new PathCount("/dining/commons/v1/", 2),
            new PathCount("/dining/menu/v1/2024-08-16/carrillo", 1)),
        pathCounts);
  }

  @Test
  public void path_counts_do_not_bleed_between_hosts() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("10.0.0.5", "/dining/commons/v1/");
    service.recordRequest("10.0.0.6", "/dining/menu/v1/2024-08-16/carrillo");

    List<HostCount> counts = service.getHostCounts();
    assertEquals(2, counts.size());

    HostCount first = counts.get(0);
    HostCount second = counts.get(1);
    assertEquals("10.0.0.5", first.host());
    assertEquals(List.of(new PathCount("/dining/commons/v1/", 1)), first.pathCounts());
    assertEquals("10.0.0.6", second.host());
    assertEquals(
        List.of(new PathCount("/dining/menu/v1/2024-08-16/carrillo", 1)), second.pathCounts());
  }

  @Test
  public void path_counts_are_sorted_by_path() {
    HostTrackingServiceImpl service = new HostTrackingServiceImpl();

    service.recordRequest("10.0.0.5", "/dining/menu/v1/2024-08-16/carrillo");
    service.recordRequest("10.0.0.5", "/dining/allergens/v2/carrillo");
    service.recordRequest("10.0.0.5", "/dining/commons/v1/");

    List<String> paths =
        service.getHostCounts().get(0).pathCounts().stream().map(PathCount::path).toList();
    assertEquals(
        List.of(
            "/dining/allergens/v2/carrillo",
            "/dining/commons/v1/",
            "/dining/menu/v1/2024-08-16/carrillo"),
        paths);
  }
}
