### Active Record vs Repository

- Extend `PanacheMongoEntity` for active record pattern with built-in CRUD.
- Implement `PanacheMongoRepository<T>` for repository pattern.
- Choose one pattern per entity.

### Entity Design

- Use public fields — Panache enhances access at build time.
- The `id` field (`ObjectId`) is provided by `PanacheMongoEntity`.
- BSON serialization is automatic — no codec configuration needed.

### Querying

- Simplified queries: `find("name", value)` or `find("age >", 18)`.
- Use `list()`, `stream()`, `firstResult()`, `singleResult()` to materialize.
- Pagination: `find("order by name").page(Page.of(0, 25)).list()`.

### Dev Services

- A MongoDB container starts automatically in dev/test — no config needed.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test MongoDB.
- Use `@TestTransaction` is NOT available for MongoDB — clean up data manually or use `deleteAll()` in `@BeforeEach`.

### Common Pitfalls

- Do NOT redeclare the `id` field — it is provided by `PanacheMongoEntity`.
- MongoDB Panache does NOT support JPA annotations — use BSON annotations instead.
