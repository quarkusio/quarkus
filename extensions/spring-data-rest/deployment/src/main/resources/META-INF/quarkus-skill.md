### Usage

- Add this extension for Spring Data REST compatibility — auto-generates REST endpoints from Spring Data JPA repositories.
- Repositories extending `CrudRepository` or `JpaRepository` get REST endpoints automatically.
- Customize paths with `@RepositoryRestResource(path = "people")`.

### Testing

- Use `@QuarkusTest` with REST Assured to test generated endpoints.
- Dev Services provides a database automatically.

### Common Pitfalls

- For new Quarkus applications, prefer `quarkus-hibernate-orm-rest-data-panache` — it is the native Quarkus approach.
- Not all Spring Data REST features are supported — check the guide for limitations.
