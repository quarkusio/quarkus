### Usage

- This extension provides the core datasource configuration shared by JDBC and reactive database extensions.
- Configure the default datasource with `quarkus.datasource.db-kind`, `quarkus.datasource.username`, `quarkus.datasource.password`.
- Supported `db-kind` values: `postgresql`, `mysql`, `mariadb`, `h2`, `mssql`, `oracle`, `db2`, `derby`.

### Multiple Datasources

- Define named datasources: `quarkus.datasource.<name>.db-kind=postgresql`.
- Each named datasource can have its own JDBC or reactive configuration.
- Inject with `@Inject @DataSource("name")`.

### Dev Services

- When a JDBC or reactive driver extension is on the classpath, Dev Services auto-starts a database container.
- Disable Dev Services per datasource with `quarkus.datasource.devservices.enabled=false`.

### Testing

- Use `@QuarkusTest` — Dev Services provides databases automatically in test mode.
- Override datasource config for tests with `%test.quarkus.datasource.*` properties.

### Common Pitfalls

- Do NOT set `quarkus.datasource.jdbc.url` without a profile prefix (`%prod.`) — this disables Dev Services.
- If using multiple datasources, every datasource must have `db-kind` configured explicitly.
