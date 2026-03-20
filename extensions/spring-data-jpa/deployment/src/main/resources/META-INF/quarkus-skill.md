### Spring Data JPA Repositories

- Define interfaces extending `CrudRepository<T, ID>` or `JpaRepository<T, ID>`.
- Quarkus generates implementations at build time.

### Query Methods

- Derive queries from method names: `findByName(String name)`, `findByAgeGreaterThan(int age)`.
- Custom queries: `@Query("SELECT e FROM Entity e WHERE e.status = ?1")`.

### Quarkus Specifics

- Uses Hibernate ORM under the hood — same `@Entity` annotations.
- Dev Services databases work the same as with Hibernate ORM.

### Testing

- Use `@QuarkusTest` with `@TestTransaction` — same as Hibernate ORM tests.

### Common Pitfalls

- Not all Spring Data features are supported — check the Quarkus compatibility guide.
- Consider Panache as the Quarkus-native alternative.
