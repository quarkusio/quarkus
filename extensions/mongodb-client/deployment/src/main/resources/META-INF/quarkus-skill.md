### Client Injection

- Inject `MongoClient` for imperative (blocking) operations.
- Inject `ReactiveMongoClient` for reactive (Mutiny-based) operations.
- Use `client.getDatabase("mydb").getCollection("mycol")` to access collections.

### Dev Services

- A MongoDB container starts automatically in dev/test mode — no config needed.
- Set `quarkus.mongodb.connection-string` with `%prod.` prefix for production only.

### Codec Configuration

- Use POJO codecs: implement `CodecProvider` or annotate POJOs with `@BsonId`, `@BsonProperty`.
- Consider MongoDB with Panache (`quarkus-mongodb-panache`) for simplified data access.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test MongoDB automatically.
- Inject `MongoClient` in tests for direct database assertions.

### Common Pitfalls

- Do NOT set `quarkus.mongodb.connection-string` without a profile prefix — this disables Dev Services.
- For reactive usage, always use `ReactiveMongoClient`, not the blocking client on reactive threads.
