### Usage

- Add this extension for non-blocking IBM Db2 access using the Vert.x reactive client.
- Inject `DB2Pool` or `io.vertx.mutiny.db2client.DB2Pool` to execute queries.
- Dev Services auto-starts a Db2 container in dev and test mode.

### Querying

- Use `client.preparedQuery("SELECT ...").execute()` for parameterized queries.
- Use `Tuple.of(...)` for query parameters.
- Results are returned as `RowSet<Row>`.

### Testing

- Use `@QuarkusTest` — Dev Services provides a Db2 container automatically.

### Common Pitfalls

- Do NOT block on reactive operations — use Mutiny operators or return `Uni`/`Multi` from endpoints.
- Do NOT mix JDBC and reactive clients for the same datasource.
