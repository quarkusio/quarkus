
## Key Patterns

### Annotation-based caching

Apply `@CacheResult`, `@CacheInvalidate`, and `@CacheInvalidateAll` to **non-private** methods of CDI beans. The `cacheName` is required on every annotation.

```java
@ApplicationScoped
public class MyService {

    @CacheResult(cacheName = "my-cache")
    public ExpensiveResult compute(String key) {
        return doExpensiveWork(key);
    }

    @CacheInvalidate(cacheName = "my-cache")
    public void invalidateOne(String key) {
        // body can be empty — invalidation is handled by the interceptor
    }

    @CacheInvalidateAll(cacheName = "my-cache")
    public void invalidateAll() {
    }
}
```

### Composite cache keys

When a method has multiple parameters, **all unannotated parameters** form the cache key. Use `@CacheKey` to select specific parameters. Multiple `@CacheKey` parameters create a `CompositeCacheKey` — **parameter order matters** and must match between `@CacheResult` and `@CacheInvalidate`:

```java
@CacheResult(cacheName = "weather")
public Weather get(@CacheKey String city, @CacheKey String units, RequestContext ctx) {
    // key = CompositeCacheKey(city, units) — ctx is excluded
}

@CacheInvalidate(cacheName = "weather")
public void invalidate(@CacheKey String city, @CacheKey String units) {
    // MUST use same parameter order as @CacheResult
}
```

### Programmatic API

Inject a cache by name with `@CacheName` or look it up via `CacheManager`:

```java
@CacheName("my-cache")
Cache cache;

@Inject
CacheManager cacheManager; // getCacheNames() returns Collection<String>, not Set
```

Access the underlying Caffeine cache for advanced operations:

```java
CaffeineCache caffeine = cache.as(CaffeineCache.class);
Set<Object> keys = caffeine.keySet();
CompletableFuture<Object> value = caffeine.getIfPresent(key);

// Programmatic put and invalidation
caffeine.put(key, CompletableFuture.completedFuture(value));
cache.invalidateAll().await().indefinitely();
cache.invalidate(key).await().indefinitely();
```

## Cache Configuration (Caffeine)

Caffeine is the default backend. Configure per-cache in `application.properties`:

```properties
quarkus.cache.caffeine."weather".maximum-size=100
quarkus.cache.caffeine."weather".expire-after-write=5M
quarkus.cache.caffeine."weather".expire-after-access=2M
```

Duration format: `S` (seconds), `M` (minutes), `H` (hours), or ISO-8601 (`PT5M`). For distributed caching, use `quarkus-redis-cache` or `quarkus-infinispan-cache`.

### Reactive Support

`@CacheResult` works with `Uni<T>` return types — the cache stores the resolved value, not the Uni itself.

## Common Pitfalls

- **Self-invocation does not trigger caching.** Calling a `@CacheResult` method from within the same bean bypasses the interceptor. Always call cached methods through the CDI proxy (from another bean or via `self` injection).
- **`CacheManager.getCacheNames()` returns `Collection<String>`**, not `Set<String>`. Using `Set` causes a compilation error.
- **`@CacheInvalidate` method bodies can be empty.** The invalidation happens in the interceptor, not in your code.
- **Cache key mismatch silently fails.** If `@CacheInvalidate` parameters don't match the `@CacheResult` key structure (same types, same order), invalidation has no effect with no error.

## Testing

Use `@QuarkusTest` normally — caching is enabled by default in all profiles. To verify caching behavior, use the programmatic API:

```java
@QuarkusTest
class CacheTest {

    @CacheName("my-cache")
    Cache cache;

    @BeforeEach
    void clearCache() {
        cache.invalidateAll().await().indefinitely();
    }

    @Test
    void verifyCacheHit() {
        // call the service twice, assert same result
        // verify key exists: cache.as(CaffeineCache.class).keySet()
    }
}
```

Prefer asserting on cache contents (`keySet()`, `getIfPresent()`) over timing-based assertions, which are flaky in CI.
