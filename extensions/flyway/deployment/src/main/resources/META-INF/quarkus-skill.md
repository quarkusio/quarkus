
## Key Patterns

### Migration file location and naming

Place SQL migrations in `src/main/resources/db/migration/`. Naming convention uses **double underscores**:

```
V1.0.0__Create_tables.sql       # versioned (runs once, in order)
V1.0.1__Add_column.sql
R__Create_views.sql              # repeatable (re-runs when content changes)
```

Repeatable migrations (`R__`) run after all versioned migrations and re-execute whenever their checksum changes. Use them for views, stored procedures, and functions.

### Essential configuration

```properties
# Required — migrations don't run automatically without this
quarkus.flyway.migrate-at-start=true

# Flyway owns the schema — disable Hibernate schema management
quarkus.hibernate-orm.schema-management.strategy=none

# Dev mode — clean and recreate schema on each restart (NEVER in prod)
%dev.quarkus.flyway.clean-at-start=true
%test.quarkus.flyway.clean-at-start=true
```

Quarkus Dev Services (from the JDBC extension) auto-starts a database container in dev/test mode — no JDBC URL needed.

### Multiple datasources

Configure named datasources with separate migration directories:

```properties
quarkus.datasource.audit.db-kind=postgresql
quarkus.flyway.audit.migrate-at-start=true
quarkus.flyway.audit.locations=db/audit
```

Place migrations in `src/main/resources/db/audit/`. Inject named Flyway beans:

```java
@Inject
@FlywayDataSource("audit")
Flyway auditFlyway;
```

### Integration with Hibernate ORM Panache

When using Flyway to manage schema AND Panache for entities, the ID generation strategy must match:

**SERIAL/BIGSERIAL columns — use IDENTITY strategy:**
```sql
-- V1.0.0__Create_task_table.sql
CREATE TABLE task (id BIGSERIAL PRIMARY KEY, title VARCHAR(255));
```
```java
@Entity
public class Task extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String title;
}
```

**Explicit sequence — use PanacheEntity (sequence-based ID generation):**
```sql
CREATE SEQUENCE task_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE task (id BIGINT PRIMARY KEY, title VARCHAR(255));
```
```java
@Entity
public class Task extends PanacheEntity {
    public String title;
}
```

### Programmatic access

Inject the `Flyway` bean to query migration status:

```java
@Inject
Flyway flyway;

public MigrationInfo[] getMigrationInfo() {
    return flyway.info().all();
}
```

### Callbacks

Implement `org.flywaydb.core.api.callback.Callback` and register via configuration. Callback classes must have a no-arg constructor and must not be abstract:

```properties
quarkus.flyway.callbacks=com.example.MigrationCallback
```

```java
public class MigrationCallback implements Callback {
    private static final Logger LOG = Logger.getLogger(MigrationCallback.class);
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_EACH_MIGRATE;
    }
    public boolean canHandleInTransaction(Event event, Context context) { return true; }
    public void handle(Event event, Context context) {
        LOG.infof("Migrated: %s", context.getMigrationInfo().getDescription());
    }
    public String getCallbackName() { return "MigrationCallback"; }
}
```

## Common Pitfalls

- **`PanacheEntity` + `BIGSERIAL` = broken.** `PanacheEntity` expects a sequence named `{table}_seq`. `BIGSERIAL` creates `{table}_id_seq`. Use `PanacheEntityBase` with `GenerationType.IDENTITY` instead.
- **`migrate-at-start` defaults to `false`.** Migrations won't run until you set `quarkus.flyway.migrate-at-start=true`.
- **`clean-at-start` is destructive.** Only use with `%dev` or `%test` profile prefix. Set `quarkus.flyway.clean-disabled=true` in production as a safety net.
- **Double underscore in filenames.** `V1__Name.sql` not `V1_Name.sql`. Single underscore causes Flyway to ignore the file silently.

## Testing

Flyway migrations run automatically in `@QuarkusTest` when Dev Services provides the database:

```java
@QuarkusTest
class MigrationTest {

    @Test
    void testSeedDataExists() {
        given()
            .when().get("/tasks")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(3));
    }
}
```

Use `%test.quarkus.flyway.clean-at-start=true` for test isolation — each test run starts with a fresh schema plus seed migrations.
