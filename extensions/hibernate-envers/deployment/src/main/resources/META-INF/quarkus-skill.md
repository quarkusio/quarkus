### Entity Auditing

- Add `@Audited` on entities to track all changes (insert, update, delete).
- Envers creates audit tables automatically (e.g. `my_entity_AUD`).

### Querying Audit Data

- Inject `AuditReader` via `AuditReaderFactory.get(entityManager)`.
- Query revisions: `reader.find(MyEntity.class, id, revisionNumber)`.
- List revisions: `reader.getRevisions(MyEntity.class, id)`.

### Configuration

- Schema generation (`drop-and-create`) creates audit tables automatically.
- For production, include audit tables in Flyway/Liquibase migrations.

### Common Pitfalls

- `@Audited` on an entity audits ALL fields — use `@NotAudited` to exclude fields.
- Audit tables grow continuously — plan for storage.
