### Usage

- Agroal is the default JDBC connection pool in Quarkus — it is included automatically when you add any JDBC driver extension.
- Configure pool size with `quarkus.datasource.jdbc.min-size` and `quarkus.datasource.jdbc.max-size`.
- Inject `javax.sql.DataSource` or `AgroalDataSource` for direct JDBC access.

### Multiple Datasources

- Define named datasources with `quarkus.datasource.<name>.jdbc.url` etc.
- Inject named datasources with `@Inject @DataSource("name") AgroalDataSource ds`.

### Health and Metrics

- Agroal auto-registers health checks and metrics when `quarkus-smallrye-health` or `quarkus-micrometer` are present.

### Testing

- Use `@QuarkusTest` — Dev Services provides database containers automatically.
- Connection pooling works transparently in tests.

### Common Pitfalls

- Do NOT set pool max-size too low for concurrent workloads — this causes connection timeouts.
- Do NOT create `DataSource` manually — always inject it via CDI.
- Connection leak detection can be enabled with `quarkus.datasource.jdbc.detect-statement-leaks=true`.
