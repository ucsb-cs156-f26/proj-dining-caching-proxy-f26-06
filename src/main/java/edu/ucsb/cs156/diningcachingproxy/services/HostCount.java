package edu.ucsb.cs156.diningcachingproxy.services;

import java.util.List;

/**
 * A host (hostname if resolved, else IP address), how many requests it has sent in total, and a
 * breakdown of that total by which request path it called.
 */
public record HostCount(String host, long count, List<PathCount> pathCounts) {}
