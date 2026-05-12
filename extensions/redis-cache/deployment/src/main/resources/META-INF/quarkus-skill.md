
### Cache Annotations

Uses the standard Quarkus Cache annotations with Redis as the backend:

```java
@ApplicationScoped
public class WeatherService {

    @CacheResult(cacheName = "weather")
    public WeatherData getWeather(String city) {
        return fetchFromApi(city); // called only on cache miss
    }

    @CacheInvalidate(cacheName = "weather")
    public void invalidateCity(String city) {
        // city parameter matches the cache key
    }

    @CacheInvalidateAll(cacheName = "weather")
    public void invalidateAll() { }
}
```

By default, **all method parameters** form the cache key. Use `@CacheKey` on specific parameters to exclude others:

```java
@CacheResult(cacheName = "weather")
public WeatherData getWeather(@CacheKey String city, String requestId) {
    // only 'city' is the cache key; requestId is ignored for caching
}
```

### Programmatic Cache Access

Inject `CacheManager` and cast to `RedisCache` for direct operations:

```java
@Inject CacheManager cacheManager;

RedisCache cache = (RedisCache) cacheManager.getCache("weather").orElseThrow();

// typed get with value loader (cache-aside)
Uni<WeatherData> data = cache.get("london", WeatherData.class, k -> fetchFromApi(k));

// put directly
cache.put("london", weatherData).await().indefinitely();

// get or null (no loading)
Uni<WeatherData> maybe = cache.getOrNull("london", WeatherData.class);

// get with default
Uni<WeatherData> withDefault = cache.getOrDefault("london", WeatherData.class, fallback);
```

Always pass the `Class<V>` parameter on programmatic calls — without it, the cache cannot deserialize the JSON stored in Redis.

### Configuration

```properties
# Per-cache expiration (use expire-after-write, NOT ttl which is deprecated)
quarkus.cache.redis.weather.expire-after-write=60s
quarkus.cache.redis.weather.expire-after-access=30m

# Value/key type (build-time, needed for programmatic access without Class param)
quarkus.cache.redis.weather.value-type=com.example.WeatherData
quarkus.cache.redis.weather.key-type=java.lang.String

# Use a named Redis client (if multiple Redis connections)
quarkus.cache.redis.client-name=cache-redis

# Key prefix (default: "cache:{cache-name}")
quarkus.cache.redis.weather.prefix=myapp:weather
```

Global defaults apply to all caches; per-cache config (`quarkus.cache.redis.{cache-name}.*`) overrides them.

### Dev Services

Redis Dev Services starts automatically — no manual Redis setup needed in dev/test mode. A `redis:7` container is started with an auto-assigned port.

### Testing

```java
@QuarkusTest
class WeatherServiceTest {

    @Inject WeatherService service;
    @Inject CacheManager cacheManager;

    @Test
    void cachedResultReturnsSameValue() {
        String first = service.getWeather("london");
        String second = service.getWeather("london");
        assertEquals(first, second); // same instance from cache
    }

    @Test
    void invalidateClearsCache() {
        service.getWeather("london");
        service.invalidateCity("london");
        // next call will miss cache and fetch fresh data
    }
}
```

### Common Pitfalls

- **`ttl` is deprecated** — use `expire-after-write` instead (`ttl` will be removed after Quarkus 3.20).
- **Missing Class parameter in programmatic API** — `cache.get(key, valueLoader)` requires `value-type` to be configured; prefer `cache.get(key, MyClass.class, valueLoader)` for explicit typing.
- **Cache key includes all parameters** — if a method has parameters that shouldn't be part of the key (like request IDs or context), annotate the key parameters with `@CacheKey`.
- **Serialization** — values are stored as JSON in Redis. Ensure cached objects are JSON-serializable. Complex generic types may need `value-type` configuration.
- **Cache annotations must be on CDI bean methods** — `@CacheResult`, `@CacheInvalidate`, and `@CacheInvalidateAll` won't work on private methods or classes not managed by CDI.
