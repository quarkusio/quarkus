### Migration Files

- Place SQL migration files in `src/main/resources/db/migration/`.
- Name files `V1__description.sql`, `V2__add_users.sql` (version double-underscore description).
- Migrations run automatically on application startup.

### Configuration

- `quarkus.flyway.migrate-at-start=true` (default) — run migrations on startup.
- `quarkus.flyway.baseline-on-migrate=true` — baseline an existing database.
- `quarkus.flyway.clean-at-start=true` — drop all objects before migrating (dev only).

### Dev Services

- Works with Dev Services databases — migrations run on the auto-provisioned database.
- No additional configuration needed in dev/test mode.

### Multiple Datasources

- Configure per-datasource: `quarkus.flyway."datasource-name".migrate-at-start=true`.

### Testing

- Tests use Dev Services + Flyway automatically — schema is migrated before tests run.
- Use `quarkus.flyway.clean-at-start=true` in test profile for clean schema each run.

### Common Pitfalls

- Do NOT modify already-applied migration files — Flyway checksums will fail.
- Do NOT use `clean-at-start` in production — it drops ALL database objects.
- Version numbers must be unique and sequential.
