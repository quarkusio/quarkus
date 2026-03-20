### Reactive Entities

- Same `@Entity` annotations as Hibernate ORM.
- Requires a reactive database client (e.g. `quarkus-reactive-pg-client`).
- Do NOT use JDBC drivers with Hibernate Reactive.

### Session and Transactions

- Inject `Mutiny.SessionFactory` for programmatic access.
- Use `@WithTransaction` on CDI methods for declarative reactive transactions.
- All operations return `Uni<T>` or `Multi<T>`.

### Testing

- Use `@QuarkusTest` with Dev Services.
- Use `@RunOnVertxContext` for tests needing the Vert.x event loop.

### Common Pitfalls

- Do NOT use `@Transactional` (JTA) — use `@WithTransaction` (Hibernate Reactive).
- Do NOT mix JDBC and reactive drivers in the same project.
- Do NOT call blocking operations in reactive transaction blocks.
