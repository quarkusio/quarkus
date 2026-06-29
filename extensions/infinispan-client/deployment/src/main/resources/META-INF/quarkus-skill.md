
### Setup

Add `quarkus-infinispan-client`. Dev Services auto-starts an Infinispan container — no manual config needed in dev/test.

### Protobuf Serialization

All cached types must be annotated for Protobuf serialization:

```java
import org.infinispan.protostream.annotations.Proto;

@Proto
public record Product(String id, String name, double price, String category) {}
```

Register a schema:

```java
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(includeClasses = { Product.class }, schemaPackageName = "com.example")
public interface ProductSchema extends GeneratedSchema {}
```

- `@Proto` on records/classes — generates Protobuf mappings at build time.
- `@ProtoSchema` on an interface extending `GeneratedSchema` — registers all types.
- `schemaPackageName` should match your Java package.

### Injecting a Remote Cache

```java
import io.quarkus.infinispan.client.Remote;
import org.infinispan.client.hotrod.RemoteCache;

@Inject
@Remote("products")
RemoteCache<String, Product> cache;
```

- `@Remote("cache-name")` is from `io.quarkus.infinispan.client` — NOT from the Infinispan library.
- The cache is auto-created on first access if it doesn't exist.

### CRUD Operations

```java
cache.put(id, product);                     // create/update
Product p = cache.get(id);                  // read (null if not found)
cache.remove(id);                           // delete
Map<String, Product> all = cache.getAll(cache.keySet()); // list all (small caches only)
cache.put(id, product, 30, TimeUnit.MINUTES); // put with TTL
```

### Programmatic Cache Access

```java
@Inject RemoteCacheManager cacheManager;

RemoteCache<String, Product> cache = cacheManager.getCache("products");
```

### Ickle Queries

For queries and listeners, use `RemoteCacheManager.getCache()` instead of `@Remote` injection (CDI proxy breaks `Search.getQueryFactory()`):

```java
@Inject RemoteCacheManager cacheManager;

public List<Product> searchByCategory(String category) {
    RemoteCache<String, Product> cache = cacheManager.getCache("products");
    QueryFactory queryFactory = org.infinispan.client.hotrod.Search.getQueryFactory(cache);
    Query<Product> query = queryFactory.create("FROM com.example.Product WHERE category = :cat");
    query.setParameter("cat", category);
    return query.execute().list();
}
```

The fully qualified Protobuf type name uses `schemaPackageName` + class name. Requires the cache to have `application/x-protostream` encoding.

### Cache Configuration

For custom cache config (encoding, expiration, indexing), create an XML file:

```xml
<!-- src/main/resources/infinispan/products.xml -->
<replicated-cache name="products">
    <encoding media-type="application/x-protostream"/>
    <expiration lifespan="3600000"/>
</replicated-cache>
```

Reference it: `quarkus.infinispan-client.devservices.config-files=infinispan/products.xml`

### Docker/Podman Networking

If Dev Services returns internal container IPs causing timeouts, add:

```properties
quarkus.infinispan-client.client-intelligence=BASIC
```

This prevents the client from using cluster topology information and sticks to the configured host. Required in most Docker/Podman environments.

### Testing

- Dev Services provides a real Infinispan server — no mocking needed.
- Clear caches in `@BeforeEach` for test isolation: `cache.clear()`.
- Use `Awaitility` for async operations or TTL-based tests.

### Common Pitfalls

- `@Remote` is from `io.quarkus.infinispan.client.Remote`, NOT from the Infinispan library — easy to get the wrong import.
- Set `client-intelligence=BASIC` in Docker/Podman environments to avoid connection timeouts to internal container IPs.
- Auto-created caches use default encoding (`application/x-java-object`) — for Protobuf queries, explicitly configure `application/x-protostream` encoding.
- `cache.getAll(cache.keySet())` works for listing all entries but is expensive for large caches — use queries instead.
- `@Proto` works on records and classes. Records need all fields in the constructor.
- For queries and listeners, use `cacheManager.getCache("name")` — the CDI proxy from `@Remote` breaks `Search.getQueryFactory()` and `cache.addClientListener()`.
