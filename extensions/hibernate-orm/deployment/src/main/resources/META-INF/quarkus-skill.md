### Entity Mapping

- Annotate entities with `@Entity` from `jakarta.persistence`.
- Use `@Id` with `@GeneratedValue` for primary keys.
- Use `@Column`, `@Table`, `@ManyToOne`, `@OneToMany`, etc. for mapping.
- Consider using Panache (`quarkus-hibernate-orm-panache`) for simplified data access.

### Session Usage

- Prefer `Session` to `EntityManager`, unless specifically instructed to follow standards.
- Use `StatelessSession` for simple CRUD scenarios, and `Session` for more involved business methods. Avoid mixing the two in a single transaction.
- Inject `Session` or `StatelessSession` with `@Inject`.
- Use `session.persist()`, `session.find()`, `session.remove()` or `statelessSession.insert()`, `statelessSession.get()`, `statelessSession.update()`, `statelessSession.delete()` for CRUD.
- Favor DTOs for REST endpoints, especially if the REST API is more limited than the domain model (only some attributes exposed in most endpoints).
- Use `session.merge()` only if a REST endpoint is accepting a serialized entity as input.
- Use `session.createSelectionQuery()` or `@NamedQuery` for JPQL queries. If Jakarta Data is in the classpath, use that in priority for static queries — especially if they are used from multiple places.
- Use DTO projections in simple read endpoints: `session.createSelectionQuery("select ...", MyDTO.class)`.
- Use entity graphs in more involved read endpoints to ensure all relevant attributes are read eagerly.

### Transactions

- Annotate service methods with `@Transactional` (from `jakarta.transaction`).
- Place `@Transactional` on the service/boundary layer, NOT on entities or repositories.

### Dev Services

- When a JDBC driver extension is on the classpath, Quarkus auto-starts a database in dev/test mode.
- In test/dev mode, if not using dev services, set `quarkus.hibernate-orm.schema-management.strategy=drop-and-create` for schema generation. If using dev services, this is set automatically by Quarkus. Note: the legacy property `quarkus.hibernate-orm.database.generation` still works but is deprecated — always use `schema-management.strategy`.
- For production, use Flyway or Liquibase for migrations.

### Testing

- Use `@QuarkusTest` — Dev Services provides a test database automatically.
- Use `@TestTransaction` to auto-rollback database changes after each test.
- Inject `Session`/`StatelessSession` in tests for direct database assertions.

### Common Pitfalls

- Do NOT use `@Transactional` on private methods — CDI proxies cannot intercept them.
- Hibernate Reactive and Hibernate ORM can coexist in the same project, but do NOT mix them in the same method — they use separate persistence contexts.
- Always add a JDBC driver extension when using Hibernate ORM, a Vert.x SQL client extension when using Hibernate Reactive.
- Define `quarkus.datasource.db-kind` for the default datasource if there are multiple JDBC drivers or Vert.x SQL client extensions in the classpath.
- Always define `quarkus.datasource."my-datasource".db-kind` for named datasources; this ensures the datasource is detected by Quarkus at build time.
