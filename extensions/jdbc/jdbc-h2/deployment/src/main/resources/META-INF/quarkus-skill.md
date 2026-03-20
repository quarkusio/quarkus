### Usage

- Add this extension to use H2 as your JDBC database.
- H2 is ideal for development, testing, and lightweight applications.
- Dev Services auto-starts an H2 database in dev and test mode — no configuration needed.

### Testing

- Use `@QuarkusTest` — Dev Services provides an H2 instance automatically.
- H2 runs in-process by default, making tests fast.

### Common Pitfalls

- Do NOT use H2 in production unless you understand its limitations (single-writer, in-process).
- H2 SQL dialect differs from PostgreSQL/MySQL — queries that work on H2 may fail on production databases.
- Prefer `quarkus-jdbc-postgresql` with Dev Services for development that matches production.
