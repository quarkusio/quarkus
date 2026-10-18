
### Setup

Add `quarkus-hibernate-reactive` + a reactive SQL client (`reactive-pg-client`, `reactive-mysql-client`, etc.). Dev Services auto-starts a database container and manages the schema — no manual config needed in dev/test.

### Entities

Standard JPA annotations — same as Hibernate ORM:

```java
@Entity
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String title;
    public boolean completed;
    @CreationTimestamp
    public LocalDateTime createdAt;
}
```

### Using @Transactional with Session

The simplest approach — inject `Mutiny.Session` directly and use `@Transactional`. Methods must return `Uni`:

```java
@Inject Mutiny.Session session;

@Transactional
public Uni<Task> create(Task task) {
    return session.persist(task).replaceWith(task);
}

@Transactional
public Uni<Task> findById(Long id) {
    return session.find(Task.class, id);
}

@Transactional
public Uni<List<Task>> listAll() {
    return session.createQuery("FROM Task", Task.class).getResultList();
}

@Transactional
public Uni<Task> update(Long id, Task updated) {
    return session.find(Task.class, id)
        .onItem().ifNotNull().invoke(existing -> {
            existing.title = updated.title;
            existing.completed = updated.completed;
        });
}

@Transactional
public Uni<Boolean> delete(Long id) {
    return session.find(Task.class, id)
        .onItem().ifNotNull().transformToUni(task ->
            session.remove(task).replaceWith(true))
        .onItem().ifNull().continueWith(false);
}
```

### SessionFactory API (alternative)

Use `Mutiny.SessionFactory` when you need explicit transaction control:

```java
@Inject Mutiny.SessionFactory sessionFactory;

public Uni<Task> create(Task task) {
    return sessionFactory.withTransaction(session ->
        session.persist(task).replaceWith(task)
    );
}

public Uni<Task> update(Long id, Task updated) {
    return sessionFactory.withTransaction(session ->
        session.find(Task.class, id)
            .onItem().ifNotNull().invoke(existing -> {
                existing.title = updated.title;
                existing.completed = updated.completed;
            })
    );
}
```

### Key Patterns

- `@Inject Mutiny.Session` + `@Transactional` (preferably) or `@Inject Mutiny.SessionFactory` + `sessionFactory.withTransaction(session -> ...)` (legacy code or advanced usage) — automatically commits on success, rolls back on exception. Changes to managed entities are flushed automatically on commit.
- `session.persist(entity)` returns `Uni<Void>` — chain `.replaceWith(entity)` to get the entity back.
- `session.find(Type.class, id)` returns `Uni<Type>` — may resolve to `null` if not found.

### Lazy Loading

Lazy initialization is not supported in Hibernate Reactive. Fetch lazy associations explicitly using `Mutiny.fetch()`:

```java
// Returns Uni<Post> with comments initialized
return session.find(Post.class, id)
    .call(post -> Mutiny.fetch(post.getComments()));

// Returns Uni<List<Comment>> directly
return session.find(Post.class, id)
    .chain(post -> Mutiny.fetch(post.getComments()));
```

- `.call()` — fetches the association, returns the parent entity (Post) with the collection initialized.
- `.chain()` — fetches the association, returns the fetched collection (List\<Comment>) directly.

Accessing an unfetched lazy collection throws `LazyInitializationException`.

### Named Queries

```java
@Entity
@NamedQuery(name = "Task.findCompleted", query = "FROM Task WHERE completed = true")
public class Task { ... }

// Usage
session.createNamedQuery("Task.findCompleted", Task.class).getResultList()
```

### Testing

- Dev Services provide a real database — no mocking needed.
- Use `@QuarkusTest` normally with REST Assured.
- Schema and test data are recreated per test run (but shared between test classes) when using Dev Services.

### Common Pitfalls

- `session.persist()` returns `Uni<Void>`, not the entity — use `.replaceWith(entity)`.
- `session.find()` returns null (not an exception) for missing entities — handle with `.onItem().ifNull()`.
- Lazy collections throw `LazyInitializationException` if not fetched — always use `Mutiny.fetch()` before accessing them, or use DTOs to control which data is serialized.
- Do NOT use `@ReactiveTransactional` — it is deprecated. Use `@Transactional` instead.
- Do NOT mix `@Transactional` with Panache's `@WithTransaction`/`@WithSession` in the same reactive pipeline.
- Do NOT block inside reactive chains — no `Thread.sleep()`, no blocking I/O.
