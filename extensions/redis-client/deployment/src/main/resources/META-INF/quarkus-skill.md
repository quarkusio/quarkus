### Client Injection

- Inject `RedisDataSource` for imperative access (key-value, hash, list, set, sorted-set, etc.).
- Inject `ReactiveRedisDataSource` for reactive (Mutiny-based) access.
- Use typed APIs: `ds.value(String.class).get("key")`, `ds.hash(MyType.class).hgetall("key")`.

### Dev Services

- A Redis container starts automatically in dev/test mode — no config needed.
- Set `quarkus.redis.hosts` with `%prod.` prefix for production only.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test Redis automatically.
- Inject `RedisDataSource` in tests for direct assertions.

### Common Pitfalls

- Do NOT set `quarkus.redis.hosts` without a profile prefix — this disables Dev Services.
- Choose the right data structure API (value, hash, list, set) for your use case.
