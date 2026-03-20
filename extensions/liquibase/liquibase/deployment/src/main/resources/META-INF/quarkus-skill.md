### ChangeLog Files

- Default location: `src/main/resources/db/changeLog.xml` (or .yaml, .sql).
- Organize changes in changeSets with unique `id` and `author`.
- Supported formats: XML, YAML, SQL, JSON.

### Configuration

- `quarkus.liquibase.migrate-at-start=true` — run migrations on startup.
- `quarkus.liquibase.change-log=db/changeLog.xml` — changelog location.

### Dev Services

- Works with Dev Services databases automatically.

### Testing

- Tests use Dev Services + Liquibase — schema is migrated before tests run.
- Use contexts or labels to control which changeSets run in test mode.

### Common Pitfalls

- Do NOT modify applied changeSets — Liquibase checksums will fail.
- Use `rollback` tags in changeSets to support rollback operations.
