### Usage

- Add this extension to use Microsoft SQL Server as your JDBC database.
- Dev Services auto-starts an MSSQL container in dev and test mode — no configuration needed.
- For production, set `quarkus.datasource.jdbc.url`, `username`, and `password` with the `%prod.` profile prefix.

### Testing

- Use `@QuarkusTest` — Dev Services provides an MSSQL container automatically.
- The MSSQL Dev Services container requires accepting the EULA — this is handled automatically.

### Common Pitfalls

- Do NOT set `quarkus.datasource.jdbc.url` without a `%prod.` prefix — this disables Dev Services for dev and test modes.
- MSSQL container images are large — first startup may take longer than other databases.
