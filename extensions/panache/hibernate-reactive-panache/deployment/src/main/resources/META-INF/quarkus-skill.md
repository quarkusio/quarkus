### Reactive Panache Entities

- Extend `ReactivePanacheEntity` for active record pattern.
- Implement `ReactivePanacheRepository<T>` for repository pattern.
- All operations return `Uni<T>` or `Multi<T>`.

### Reactive Queries

- `MyEntity.findById(id)` returns `Uni<MyEntity>`.
- `MyEntity.listAll()` returns `Uni<List<MyEntity>>`.
- Use `.onItem()`, `.chain()` for Mutiny composition.

### Transactions

- Use `@WithTransaction` on CDI methods — NOT `@Transactional`.
- Persist/delete operations must be within a reactive transaction.

### Testing

- Use `@QuarkusTest` with `@RunOnVertxContext` for reactive test methods.
- Dev Services provides the test database automatically.

### Common Pitfalls

- Do NOT use `@Transactional` — it is for JTA/blocking only.
- Do NOT call `.await().indefinitely()` in production code — only in tests.
- Requires a reactive database driver (e.g. `quarkus-reactive-pg-client`).
