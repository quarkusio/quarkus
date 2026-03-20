### Usage

- Add this extension to use Oracle Database as your JDBC database.
- Dev Services auto-starts an Oracle container in dev and test mode — no configuration needed.
- For production, set `quarkus.datasource.jdbc.url`, `username`, and `password` with the `%prod.` profile prefix.

### Testing

- Use `@QuarkusTest` — Dev Services provides an Oracle container automatically.
- Oracle Dev Services uses the `gvenzl/oracle-free` container image by default.

### Common Pitfalls

- Do NOT set `quarkus.datasource.jdbc.url` without a `%prod.` prefix — this disables Dev Services for dev and test modes.
- Oracle container images are large — first startup will be significantly slower than PostgreSQL or MySQL.
