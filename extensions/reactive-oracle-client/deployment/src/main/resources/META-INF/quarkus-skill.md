### Usage

- Add this extension for non-blocking Oracle Database access using the Vert.x reactive client.
- Inject `OraclePool` or `io.vertx.mutiny.oracleclient.OraclePool` to execute queries.
- Dev Services auto-starts an Oracle container in dev and test mode.

### Querying

- Use `client.preparedQuery("SELECT ...").execute()` for parameterized queries.
- Use `Tuple.of(...)` for query parameters.
- Results are returned as `RowSet<Row>`.

### Testing

- Use `@QuarkusTest` — Dev Services provides an Oracle container automatically.

### Common Pitfalls

- Do NOT block on reactive operations — use Mutiny operators or return `Uni`/`Multi` from endpoints.
- Do NOT mix JDBC and reactive clients for the same datasource.
- Oracle container startup is slow — first test run will take longer.
