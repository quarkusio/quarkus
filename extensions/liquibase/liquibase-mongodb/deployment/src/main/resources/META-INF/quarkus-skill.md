### Usage

- Add this extension for MongoDB schema migrations using Liquibase.
- Place changelog files in `src/main/resources/db/changeLog.xml` (or `.yaml`, `.json`).
- Configure with `quarkus.liquibase-mongodb.migrate-at-start=true` to run migrations on startup.

### Changelog Format

- Use the MongoDB Liquibase extension format for changesets.
- Supported commands: `createCollection`, `createIndex`, `insertOne`, `insertMany`, `runCommand`.

### Testing

- Use `@QuarkusTest` — Dev Services provides a MongoDB container automatically.
- Set `quarkus.liquibase-mongodb.migrate-at-start=true` in `application.properties` for test mode.

### Common Pitfalls

- Do NOT mix this with the standard `quarkus-liquibase` extension — they are for different databases.
- MongoDB changesets use a different format than relational database changesets.
