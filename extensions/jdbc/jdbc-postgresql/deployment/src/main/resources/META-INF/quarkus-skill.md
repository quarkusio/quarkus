### Dev Services (Zero-Config Database)

- In dev and test mode, Quarkus automatically starts a PostgreSQL container via Dev Services — no configuration needed.
- Just add this extension to your project and a PostgreSQL database is available immediately.
- The container is shared across dev mode restarts (Testcontainers reuse).
- To see the auto-configured URL and credentials, use the Dev UI or check the log output.

### Configuration

- For production, set the datasource URL explicitly:
  ```properties
  quarkus.datasource.db-kind=postgresql
  quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
  quarkus.datasource.username=myuser
  quarkus.datasource.password=mypassword
  ```
- Use `%prod.` prefix to set production-only values while keeping Dev Services in dev/test:
  ```properties
  %prod.quarkus.datasource.jdbc.url=jdbc:postgresql://prod-host:5432/mydb
  %prod.quarkus.datasource.username=produser
  %prod.quarkus.datasource.password=prodpassword
  ```

### Multiple Datasources

- For additional datasources, use a named qualifier:
  ```properties
  quarkus.datasource."reporting".db-kind=postgresql
  quarkus.datasource."reporting".jdbc.url=jdbc:postgresql://localhost:5432/reporting
  ```
- Inject named datasources with `@io.quarkus.agroal.DataSource("reporting")`.

### Testing

- Tests use Dev Services by default — a fresh PostgreSQL container is started for the test suite.
- No test-specific datasource configuration is needed.
- Use `@QuarkusTest` and the database is available automatically:
  ```java
  @QuarkusTest
  class MyDatabaseTest {
      @Inject
      AgroalDataSource dataSource;

      @Test
      void testConnection() throws Exception {
          try (var conn = dataSource.getConnection()) {
              assertThat(conn.isValid(5)).isTrue();
          }
      }
  }
  ```
- To use a fixed port for the Dev Services container (useful for external tools), set `quarkus.datasource.devservices.port`.

### Health Checks

- When `quarkus-smallrye-health` is present, a datasource health check is registered automatically.
- The health check validates that the database connection is active.

### Common Pitfalls

- Do NOT hardcode JDBC URLs in `application.properties` without a profile prefix — this disables Dev Services.
- Do NOT set `quarkus.datasource.jdbc.url` without a `%prod.` prefix during development.
- If Dev Services fails to start, ensure Docker or Podman is running.
