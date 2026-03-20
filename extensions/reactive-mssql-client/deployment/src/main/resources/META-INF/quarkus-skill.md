### Usage

- Add this extension for non-blocking Microsoft SQL Server access using the Vert.x reactive client.
- Inject `MSSQLPool` or `io.vertx.mutiny.mssqlclient.MSSQLPool` to execute queries.
- Dev Services auto-starts an MSSQL container in dev and test mode.

### Querying

- Use `client.preparedQuery("SELECT ...").execute()` for parameterized queries.
- Use `Tuple.of(...)` for query parameters.
- Results are returned as `RowSet<Row>`.

### Testing

- Use `@QuarkusTest` — Dev Services provides an MSSQL container automatically.

### Common Pitfalls

- Do NOT block on reactive operations — use Mutiny operators or return `Uni`/`Multi` from endpoints.
- Do NOT mix JDBC and reactive clients for the same datasource.
