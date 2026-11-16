package edu.ucsb.cs156.diningcachingproxy.collections;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/** Mongo repository ("collection") for {@link CachedResponse} documents. */
@Repository
public interface CachedResponseRepository extends MongoRepository<CachedResponse, String> {
  Optional<CachedResponse> findByRequestPath(String requestPath);

  /**
   * Deletes every cached response last accessed before {@code cutoff}, plus any with no {@code
   * lastAccessedAt} at all (documents cached before that field existed; in a Mongo query, {@code
   * null} matches a missing field). Deleting a live entry only costs one extra upstream request the
   * next time it's asked for, so treating "unknown" as "stale" is safe.
   *
   * @return the number of documents deleted
   */
  @DeleteQuery("{ '$or': [ { 'lastAccessedAt': { '$lt': ?0 } }, { 'lastAccessedAt': null } ] }")
  long deleteNotAccessedSince(Instant cutoff);
}
