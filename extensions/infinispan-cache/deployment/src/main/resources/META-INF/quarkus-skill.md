### Usage

- Add this extension to use Infinispan as the backend for `@CacheResult`, `@CacheInvalidate`, and `@CacheInvalidateAll` annotations.
- Replaces the default in-memory Caffeine cache with a distributed Infinispan cache.
- Requires `quarkus-infinispan-client` on the classpath.

### Testing

- Use `@QuarkusTest` — Dev Services provides an Infinispan container automatically.

### Common Pitfalls

- Do NOT use alongside the default Caffeine cache for the same cache name.
- For local-only caching, the default `quarkus-cache` (Caffeine) is simpler and faster.
- For Redis-backed caching, use `quarkus-redis-cache` instead.
