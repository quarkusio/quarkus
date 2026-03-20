### Usage

- Add this extension for non-blocking MySQL database access using the Vert.x reactive client.
- Inject `MySQLPool` or `io.vertx.mutiny.mysqlclient.MySQLPool` to execute queries.
- Dev Services auto-starts a MySQL container in dev and test mode.

### Querying

- Use `client.preparedQuery("SELECT ...").execute()` for parameterized queries.
- Use `Tuple.of(...)` for query parameters.
- Results are returned as `RowSet<Row>` — iterate with `.iterator()` or convert with `.toMulti()`.

### Testing

- Use `@QuarkusTest` — Dev Services provides a MySQL container automatically.
- Inject `MySQLPool` in tests for direct assertions.

### Common Pitfalls

- Do NOT block on reactive operations — use Mutiny operators (`onItem()`, `chain()`) or return `Uni`/`Multi` from endpoints.
- Do NOT mix JDBC and reactive clients for the same datasource — choose one approach.
