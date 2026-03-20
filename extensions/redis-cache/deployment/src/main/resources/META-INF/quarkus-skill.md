### Usage

- Add this extension to use Redis as the backend for `@CacheResult`, `@CacheInvalidate`, and `@CacheInvalidateAll` annotations.
- Replaces the default in-memory Caffeine cache with a distributed Redis cache.
- Requires `quarkus-redis-client` on the classpath.

### Configuration

- Configure cache names: `quarkus.cache.redis.my-cache.ttl=10M` (10 minutes TTL).
- Each cache name can have its own TTL and configuration.

### Testing

- Use `@QuarkusTest` — Dev Services provides a Redis container automatically.
- Cache behavior is transparent in tests.

### Common Pitfalls

- Do NOT use alongside the default Caffeine cache for the same cache name — choose one backend.
- Cached values must be serializable to JSON.
- For local-only caching, the default `quarkus-cache` (Caffeine) extension is simpler and faster.
