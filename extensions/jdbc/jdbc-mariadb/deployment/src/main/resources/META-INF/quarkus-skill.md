### Usage

- Add this extension to use MariaDB as your JDBC database.
- Dev Services auto-starts a MariaDB container in dev and test mode — no configuration needed.
- For production, set `quarkus.datasource.jdbc.url`, `username`, and `password` with the `%prod.` profile prefix.

### Testing

- Use `@QuarkusTest` — Dev Services provides a MariaDB container automatically.
- Use `@TestTransaction` to roll back database changes after each test.

### Common Pitfalls

- Do NOT set `quarkus.datasource.jdbc.url` without a `%prod.` prefix — this disables Dev Services for dev and test modes.
- MariaDB is wire-compatible with MySQL but has dialect differences — do not assume full interchangeability.
