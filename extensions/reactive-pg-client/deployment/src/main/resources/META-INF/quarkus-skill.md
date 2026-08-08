
### Injecting the Pool

```java
@Inject io.vertx.mutiny.pgclient.PgPool client;
```

The Mutiny API (`io.vertx.mutiny.*`) is recommended for most users — it returns `Uni`/`Multi` and integrates naturally with Quarkus reactive. The bare Vert.x API (`io.vertx.pgclient.*`) is also valid but uses Vert.x `Future` instead.

### Queries with Prepared Statements

```java
// SELECT with parameters (PostgreSQL uses $1, $2, ... placeholders)
Uni<RowSet<Row>> result = client.preparedQuery("SELECT * FROM tasks WHERE id = $1")
    .execute(Tuple.of(id));

// INSERT with RETURNING
Uni<Row> inserted = client.preparedQuery(
    "INSERT INTO tasks (title, completed) VALUES ($1, $2) RETURNING *")
    .execute(Tuple.of(title, false))
    .onItem().transform(RowSet::iterator)
    .onItem().transform(Iterator::next);

// UPDATE
client.preparedQuery("UPDATE tasks SET completed = true WHERE id = $1")
    .execute(Tuple.of(id));

// DELETE
client.preparedQuery("DELETE FROM tasks WHERE id = $1")
    .execute(Tuple.of(id));
```

### Row Mapping

Map `Row` to domain objects manually:

```java
static Task fromRow(Row row) {
    return new Task(
        row.getLong("id"),
        row.getString("title"),
        row.getBoolean("completed"),
        row.getLocalDateTime("created_at")
    );
}
```

Available getters: `getString`, `getLong`, `getInteger`, `getBoolean`, `getDouble`, `getBigDecimal`, `getLocalDate`, `getLocalDateTime`, `getUUID`, `getJsonObject`.

### Returning Multiple Results

```java
// Return as Uni<List<Task>> — preferred for most cases
public Uni<List<Task>> listAll() {
    return client.query("SELECT * FROM tasks")
        .execute()
        .onItem().transform(rowSet -> {
            List<Task> tasks = new ArrayList<>();
            for (Row row : rowSet) tasks.add(Task.fromRow(row));
            return tasks;
        });
}

// Return as Multi<Task> — use for streaming large result sets
public Multi<Task> streamAll() {
    return client.query("SELECT * FROM tasks ORDER BY id")
        .execute()
        .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
        .onItem().transform(Task::fromRow);
}
```

### Transactions

```java
client.withTransaction(conn ->
    conn.preparedQuery("INSERT INTO users (name) VALUES ($1) RETURNING id")
        .execute(Tuple.of("Alice"))
        .onItem().transformToUni(rows -> {
            Long userId = rows.iterator().next().getLong("id");
            return conn.preparedQuery("INSERT INTO roles (user_id, role) VALUES ($1, $2)")
                .execute(Tuple.of(userId, "admin"));
        })
);
```

Automatically commits on success, rolls back on exception.

### Batch Operations

```java
List<Tuple> batch = List.of(
    Tuple.of("Alice"), Tuple.of("Bob"), Tuple.of("Charlie")
);
client.preparedQuery("INSERT INTO users (name) VALUES ($1)")
    .executeBatch(batch);
```

### Schema Initialization

```java
void onStart(@Observes StartupEvent ev) {
    client.query("CREATE TABLE IF NOT EXISTS tasks (" +
        "id SERIAL PRIMARY KEY, title TEXT NOT NULL, " +
        "completed BOOLEAN DEFAULT FALSE, " +
        "created_at TIMESTAMP DEFAULT NOW())")
        .execute().await().indefinitely();
}
```

Blocking with `.await().indefinitely()` is acceptable in `@Observes StartupEvent` — the app hasn't started serving requests yet.

### Configuration

Dev Services auto-starts PostgreSQL — no manual config needed in dev/test.

```properties
# Production datasource
quarkus.datasource.reactive.url=postgresql://localhost:5432/mydb
quarkus.datasource.username=user
quarkus.datasource.password=pass
quarkus.datasource.reactive.max-size=20
```

### Testing

- Dev Services provides a real PostgreSQL — no mocking needed.
- Schema is recreated on each startup if using `CREATE TABLE IF NOT EXISTS` or `DROP/CREATE`.
- Use REST Assured with reactive endpoints — they return immediately.

### Common Pitfalls

- Prefer `io.vertx.mutiny.*` imports for the Mutiny API (`Uni`/`Multi`). The bare Vert.x API is also valid but uses `Future`.
- PostgreSQL uses `$1, $2, $3` placeholders — not `?` like JDBC.
- `RowSet` is iterable but NOT a `List` — use `Multi.createFrom().iterable(rowSet)` to stream, or iterate manually.
- `RETURNING *` in INSERT/UPDATE returns the full row including generated columns — use it to avoid a separate SELECT.
- For schema management in production, prefer Flyway or Liquibase over manual DDL in startup events.
