### Usage

- This extension auto-generates REST CRUD endpoints from MongoDB Panache entities or repositories.
- Define an interface extending `PanacheMongoEntityResource<Entity, Id>` or `PanacheMongoRepositoryResource<Repository, Entity, Id>`.
- Quarkus generates the implementation at build time.

### Interface Pattern

```java
@ResourceProperties(hal = true)
public interface PersonResource extends PanacheMongoEntityResource<Person, ObjectId> {
    // All CRUD endpoints are generated automatically
}
```

### Testing

- Use `@QuarkusTest` with REST Assured to test the generated endpoints.
- Dev Services provides a MongoDB container automatically.

### Common Pitfalls

- Do NOT implement the interface — Quarkus generates it.
- Entities must extend `PanacheMongoEntity` or `PanacheMongoEntityBase`.
