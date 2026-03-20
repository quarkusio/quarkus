### Usage

- Add this extension for Spring Cache API compatibility in Quarkus.
- Use `@Cacheable`, `@CacheEvict`, and `@CachePut` annotations from Spring.
- The underlying cache implementation is Quarkus Cache (Caffeine by default).

### Pattern

```java
@ApplicationScoped
public class MyService {
    @Cacheable("my-cache")
    public String expensiveOperation(String key) { ... }

    @CacheEvict("my-cache")
    public void invalidate(String key) { ... }
}
```

### Testing

- Use `@QuarkusTest` — caching works transparently.

### Common Pitfalls

- For new Quarkus applications, prefer the native `@CacheResult`/`@CacheInvalidate` annotations from `quarkus-cache`.
- This extension is a compatibility layer — not all Spring Cache features are supported.
