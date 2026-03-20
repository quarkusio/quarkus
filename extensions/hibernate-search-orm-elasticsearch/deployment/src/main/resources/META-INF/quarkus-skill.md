### Indexing Entities

- Add `@Indexed` on entities to enable full-text search.
- `@FullTextField` — analyzed text field (for full-text search).
- `@KeywordField` — exact match field (for filtering/sorting).
- `@GenericField` — non-text fields (numbers, dates).

### Searching

- Inject `SearchSession` for search operations.
- Use `searchSession.search(MyEntity.class).where(f -> f.match().field("name").matching("query")).fetchHits(20)`.

### Mass Indexing

- `searchSession.massIndexer().startAndWait()` — reindex all entities.
- Run on startup or via a REST endpoint.

### Dev Services

- An Elasticsearch container starts automatically in dev/test.

### Testing

- Use `@QuarkusTest` — Dev Services provides test Elasticsearch.
- Call mass indexer in `@BeforeEach` for consistent search results.

### Common Pitfalls

- Changes to indexed entities are auto-synced, but initial data needs mass indexing.
- Do NOT set `quarkus.hibernate-search-orm.elasticsearch.hosts` without profile prefix.
