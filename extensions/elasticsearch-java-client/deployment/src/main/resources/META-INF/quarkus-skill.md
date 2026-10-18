
### Injecting the Client

```java
@Inject ElasticsearchClient client; // synchronous
@Inject ElasticsearchAsyncClient asyncClient; // async
```

Both are from the `co.elastic.clients.elasticsearch` package (Elasticsearch Java Client `co.elastic.clients:elasticsearch-java`).

### Creating an Index

```java
client.indices().create(c -> c
    .index("products"));
```

### Indexing Documents

```java
Product product = new Product(1, "laptop", 999.99);

client.index(i -> i
    .index("products")
    .id(Objects.toString(product.id())) // if a document with this ID already exists, it will be updated
    .document(product));
```

### Searching

```java
SearchResponse<Product> response = client.search(s -> s
    .index("products")
    .query(q -> q
        .match(m -> m
            .field("name")
            .query("laptop"))),
    Product.class);

List<Hit<Product>> hits = response.hits().hits();
hits.forEach(hit -> {
    Product p = hit.source();
});
```

### Configuration

```properties
# Dev Services auto-starts Elasticsearch — no config needed in dev/test
# Production (use %prod. prefix to avoid overriding Dev Services in dev/test):
%prod.quarkus.elasticsearch.hosts=localhost:9200
%prod.quarkus.elasticsearch.username=elastic
%prod.quarkus.elasticsearch.password=changeme
```

### Dev Services

An Elasticsearch container starts automatically in dev/test mode. No configuration needed.

### Common Pitfalls

- **Use `co.elastic.clients` package** — not the old `org.elasticsearch.client` (High Level REST Client is deprecated).
- **Documents must be serializable** — the client uses Jackson for serialization. Ensure POJOs have default constructors.
- **Index names must be lowercase** — Elasticsearch requires lowercase index names.
- **Dev Services uses a specific Elasticsearch version** — check compatibility with your client code if using version-specific features.
- **Async client returns `CompletableFuture`** — not Mutiny `Uni`. Wrap with `Uni.createFrom().completionStage()` if needed.
