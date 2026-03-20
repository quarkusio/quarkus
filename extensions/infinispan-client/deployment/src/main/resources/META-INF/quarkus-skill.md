### Cache Operations

- Inject `RemoteCacheManager` and get caches: `cacheManager.getCache("my-cache")`.
- Use `cache.put(key, value)`, `cache.get(key)`, `cache.remove(key)`.

### Protobuf Schemas

- Annotate POJOs with `@Proto` and `@ProtoField` for serialization.
- Or use `@ProtoSchema` on an interface to generate schemas.

### Dev Services

- An Infinispan container starts automatically in dev/test.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test Infinispan.

### Common Pitfalls

- Remote caches must be created before use — configure in Infinispan or use `@CacheCollection`.
- Protobuf schemas must be registered for complex value types.
