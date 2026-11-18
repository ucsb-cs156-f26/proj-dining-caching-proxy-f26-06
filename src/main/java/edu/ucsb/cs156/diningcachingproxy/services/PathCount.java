package edu.ucsb.cs156.diningcachingproxy.services;

/** A request path and how many times a given host has requested it. */
public record PathCount(String path, long count) {}
