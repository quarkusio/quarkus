
### Setup

Add `quarkus-liquibase` + a JDBC driver. Place changelogs in `src/main/resources/db/changeLog.xml` (the default path).

```properties
quarkus.liquibase.migrate-at-start=true
```

Dev Services auto-starts a database — no manual datasource config needed in dev/test.

### Changelog Structure

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="1" author="dev">
        <createSequence sequenceName="contacts_seq" startValue="1" incrementBy="50"/>
        <createTable tableName="contacts">
            <column name="id" type="BIGINT"><constraints primaryKey="true"/></column>
            <column name="name" type="VARCHAR(255)"><constraints nullable="false"/></column>
            <column name="email" type="VARCHAR(255)"><constraints unique="true"/></column>
        </createTable>
    </changeSet>

    <changeSet id="2" author="dev">
        <insert tableName="contacts">
            <column name="id" valueNumeric="1"/>
            <column name="name" value="Alice"/>
            <column name="email" value="alice@example.com"/>
        </insert>
        <sql>ALTER SEQUENCE contacts_seq RESTART WITH 2</sql>
    </changeSet>
</databaseChangeLog>
```

### Sequence + Panache Compatibility

When using Liquibase with Hibernate ORM Panache:
- Do **NOT** use `autoIncrement="true"` on the `id` column — it creates a database-level auto-increment but NOT the sequence Hibernate expects.
- Instead, create an explicit sequence: `<createSequence sequenceName="{table}_seq" startValue="1" incrementBy="50"/>`.
- Panache expects `{table_name}_seq` as the sequence name (e.g., `contacts_seq` for `contacts` table).
- Default `incrementBy` is 50 (Hibernate's allocationSize for batch optimization).
- After inserting seed data with explicit IDs, reset the sequence: `ALTER SEQUENCE contacts_seq RESTART WITH {next_id}`.

### Configuration

```properties
quarkus.liquibase.migrate-at-start=true          # run migrations on startup
quarkus.liquibase.change-log=db/changeLog.xml     # default path
quarkus.liquibase.contexts=production             # only run changesets with this context
quarkus.liquibase.default-schema-name=public      # target schema
quarkus.liquibase.validate-on-migrate=true        # validate checksums
```

### Multiple Datasources

```properties
# Default datasource
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.change-log=db/changeLog.xml

# Named "users" datasource
quarkus.liquibase.users.migrate-at-start=true
quarkus.liquibase.users.change-log=db/users.xml
```

### Programmatic Migration Control

```java
@Inject LiquibaseFactory liquibaseFactory;

public void migrate() throws Exception {
    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
        liquibase.update(new Contexts(), new LabelExpression());
    }
}
```

Disable auto-migration and trigger manually: `quarkus.liquibase.migrate-at-start=false`.

### Contexts

Control which changesets run per environment:

```xml
<changeSet id="3" author="dev" context="test">
    <insert tableName="contacts">...</insert>
</changeSet>
```

```properties
# Only in test profile
%test.quarkus.liquibase.contexts=test
%prod.quarkus.liquibase.contexts=production
```

### Testing

- Dev Services provides a real database — Liquibase migrations run before tests.
- Data from Liquibase seed changesets persists across test methods — test for data presence, not exact counts.
- Use `@TestTransaction` for test isolation on write tests.
- Generate unique test data (e.g., timestamp-based emails) to avoid constraint violations on re-runs.

### Common Pitfalls

- `autoIncrement="true"` does NOT create the `{table}_seq` sequence that Panache expects — use explicit `createSequence` instead.
- Changelog path defaults to `db/changeLog.xml` — a different path requires `quarkus.liquibase.change-log` config.
- `migrate-at-start` is `false` by default — set it to `true` or migrations won't run.
- Each changeset needs a unique `id` + `author` pair — duplicate IDs cause errors.
- Dev Services reuses the database container — test data accumulates across runs unless explicitly cleaned.
- Hibernate ORM's `database.generation` should be `none` when using Liquibase — don't let both manage the schema.
