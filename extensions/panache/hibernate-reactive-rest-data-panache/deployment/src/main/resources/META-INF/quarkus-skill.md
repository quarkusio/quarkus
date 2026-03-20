### Usage

- This extension auto-generates reactive REST CRUD endpoints from Hibernate Reactive Panache entities or repositories.
- Define an interface extending `PanacheEntityResource<Entity, Id>` from the reactive package.
- Quarkus generates the implementation at build time — all endpoints return reactive types.

### Testing

- Use `@QuarkusTest` with REST Assured — the reactive nature is transparent to test clients.
- Dev Services provides a database container automatically.

### Common Pitfalls

- Do NOT implement the interface — Quarkus generates it.
- Ensure you use the reactive Panache entity (`io.quarkus.hibernate.reactive.panache`), not the blocking one.
