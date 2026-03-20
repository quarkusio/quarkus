### Entity Mapping

- Annotate entities with `@Entity` from `jakarta.persistence`.
- Use `@Id` with `@GeneratedValue` for primary keys.
- Use `@Column`, `@Table`, `@ManyToOne`, `@OneToMany`, etc. for mapping.
- Consider using Panache (`quarkus-hibernate-orm-panache`) for simplified data access.

### EntityManager Usage

- Inject `EntityManager` with `@Inject`.
- Use `em.persist()`, `em.find()`, `em.merge()`, `em.remove()` for CRUD.
- Use `em.createQuery()` or `@NamedQuery` for JPQL queries.

### Transactions

- Annotate service methods with `@Transactional` (from `jakarta.transaction`).
- Place `@Transactional` on the service/boundary layer, NOT on entities or repositories.
- `@GET` endpoints typically do NOT need `@Transactional`.

### Dev Services

- When a JDBC driver extension is on the classpath, Quarkus auto-starts a database in dev/test mode.
- Set `quarkus.hibernate-orm.database.generation=drop-and-create` for dev mode schema generation.
- For production, use Flyway or Liquibase for migrations.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test database automatically.
- Use `@TestTransaction` to auto-rollback database changes after each test.
- Inject `EntityManager` in tests for direct database assertions.

### Common Pitfalls

- Do NOT use `@Transactional` on private methods — CDI proxies cannot intercept them.
- Do NOT mix Hibernate Reactive with Hibernate ORM — choose one per project.
- Always define `quarkus.datasource.db-kind` or add a JDBC driver extension.
