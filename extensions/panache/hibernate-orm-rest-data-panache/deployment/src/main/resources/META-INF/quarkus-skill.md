### Usage

- This extension auto-generates REST CRUD endpoints from Panache entities or repositories.
- Define an interface extending `PanacheEntityResource<Entity, Id>` or `PanacheRepositoryResource<Repository, Entity, Id>`.
- Quarkus generates the implementation at build time — no boilerplate needed.

### Interface Pattern

```java
@ResourceProperties(hal = true)
public interface PersonResource extends PanacheEntityResource<Person, Long> {
    // All CRUD endpoints are generated automatically
}
```

### Customization

- Use `@ResourceProperties` to configure path, HAL support, and paging.
- Use `@MethodProperties` to disable specific operations or customize them.
- Override default methods in the interface to add custom logic.

### Generated Endpoints

- `GET /persons` — list all (with paging)
- `GET /persons/{id}` — get by id
- `POST /persons` — create
- `PUT /persons/{id}` — update
- `DELETE /persons/{id}` — delete

### Testing

- Use `@QuarkusTest` with REST Assured to test the generated endpoints.
- All standard CRUD operations should be tested.

### Common Pitfalls

- Do NOT implement the interface — Quarkus generates the implementation automatically.
- Entities must extend `PanacheEntity` or use `PanacheEntityBase` with a custom ID.
- The `@Transactional` annotation is handled automatically by the generated implementation.
