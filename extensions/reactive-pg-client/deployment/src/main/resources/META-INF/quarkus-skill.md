### Reactive PostgreSQL Access

- Inject `PgPool` for reactive database access.
- Queries return `Uni<RowSet<Row>>` — use Mutiny operators to process results.
- Use `pool.preparedQuery("SELECT * FROM users WHERE id = $1").execute(Tuple.of(id))`.

### Row Mapping

- Iterate `RowSet<Row>` with `for (Row row : rowSet)` or `.toMulti()`.
- Access columns: `row.getString("name")`, `row.getInteger("age")`.

### Dev Services

- A PostgreSQL container starts automatically in dev/test.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test database.
- Use `@RunOnVertxContext` for tests that need the Vert.x event loop.

### Common Pitfalls

- Do NOT block on reactive threads — use Mutiny operators.
- This client is for reactive use — for blocking code, use JDBC instead.
