### Caching Annotations

- `@CacheResult(cacheName = "my-cache")` — cache method return values. Cache key = method arguments.
- `@CacheInvalidate(cacheName = "my-cache")` — invalidate cache entries matching arguments.
- `@CacheInvalidateAll(cacheName = "my-cache")` — clear entire cache.
- `@CacheKey` — mark specific parameters as cache key components.

### Configuration

- Default backend is Caffeine (in-memory).
- `quarkus.cache.caffeine."my-cache".maximum-size=100` — max entries.
- `quarkus.cache.caffeine."my-cache".expire-after-write=10M` — TTL.

### Testing

- Cache is active in `@QuarkusTest` — be aware of cached values across tests.
- Inject `CacheManager` to invalidate caches in `@BeforeEach` if needed.

### Common Pitfalls

- `@CacheResult` uses ALL method parameters as the key by default — use `@CacheKey` to restrict.
- Caching only works on CDI bean methods — not plain classes.
