### Usage

- Add this extension to use IBM Db2 as your JDBC database.
- Dev Services auto-starts a Db2 container in dev and test mode — no configuration needed.
- For production, set `quarkus.datasource.jdbc.url`, `username`, and `password` with the `%prod.` profile prefix.

### Testing

- Use `@QuarkusTest` — Dev Services provides a Db2 container automatically.

### Common Pitfalls

- Do NOT set `quarkus.datasource.jdbc.url` without a `%prod.` prefix — this disables Dev Services for dev and test modes.
- Db2 container images are large and slow to start — consider using PostgreSQL for development if Db2 is not strictly required.
