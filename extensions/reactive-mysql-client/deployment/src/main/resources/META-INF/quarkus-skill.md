
### Injecting the Pool

```java
@Inject io.vertx.mutiny.mysqlclient.MySQLPool client;
```

The Mutiny API (`io.vertx.mutiny.*`) is recommended for most users — it returns `Uni`/`Multi` and integrates naturally with Quarkus reactive. The bare Vert.x API (`io.vertx.mysqlclient.*`) is also valid but uses Vert.x `Future` instead.

### Queries with Prepared Statements

```java
// SELECT with parameters (MySQL uses ? placeholders)
Uni<RowSet<Row>> result = client.preparedQuery("SELECT * FROM tasks WHERE id = ?")
    .execute(Tuple.of(id));

// INSERT
client.preparedQuery("INSERT INTO tasks (title, completed) VALUES (?, ?)")
    .execute(Tuple.of(title, false));

// UPDATE
client.preparedQuery("UPDATE tasks SET completed = true WHERE id = ?")
    .execute(Tuple.of(id));

// DELETE
client.preparedQuery("DELETE FROM tasks WHERE id = ?")
    .execute(Tuple.of(id));
```

### Row Mapping

```java
static Task fromRow(Row row) {
    return new Task(
        row.getLong("id"),
        row.getString("title"),
        row.getBoolean("completed"));
}
```

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
    conn.preparedQuery("INSERT INTO users (name) VALUES (?)")
        .execute(Tuple.of("Alice"))
        .onItem().transformToUni(rows -> 
            conn.preparedQuery("INSERT INTO roles (user_id, role) VALUES (?, ?)")
                .execute(Tuple.of(userId, "admin")))
);
```

### Configuration

Dev Services auto-starts MySQL — no manual config needed in dev/test.

```properties
# Production
quarkus.datasource.reactive.url=mysql://localhost:3306/mydb
quarkus.datasource.username=user
quarkus.datasource.password=pass
```

### Common Pitfalls

- **MySQL uses `?` placeholders** — not `$1, $2` like PostgreSQL.
- Prefer `io.vertx.mutiny.*` imports for the Mutiny API (`Uni`/`Multi`). The bare Vert.x API is also valid but uses `Future`.
- **`RowSet` is iterable but NOT a `List`** — iterate manually or use `Multi.createFrom().iterable(rowSet)`.
- **No `RETURNING` clause in MySQL** — unlike PostgreSQL, MySQL doesn't support `INSERT ... RETURNING *`. Use `LAST_INSERT_ID()` for auto-generated IDs.
- **For schema management in production**, prefer Flyway or Liquibase over manual DDL in startup events.
