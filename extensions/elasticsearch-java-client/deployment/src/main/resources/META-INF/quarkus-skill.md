### Client Injection

- Inject `ElasticsearchClient` for synchronous operations.
- Inject `ReactiveElasticsearchClient` for reactive operations.

### Indexing and Searching

- Use the Java API Client fluent builders for index, search, and aggregation operations.
- Map documents to POJOs with Jackson annotations.

### Dev Services

- An Elasticsearch container starts automatically in dev/test — no config needed.
- Set `quarkus.elasticsearch.hosts` with `%prod.` prefix for production.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test Elasticsearch.

### Common Pitfalls

- Do NOT set `quarkus.elasticsearch.hosts` without profile prefix — disables Dev Services.
