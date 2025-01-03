package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.FlywayExecutor;
import org.flywaydb.core.api.output.BaselineResult;
import org.flywaydb.core.internal.callback.CallbackExecutor;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.resolver.CompositeMigrationResolver;
import org.flywaydb.core.internal.schemahistory.SchemaHistory;

public class FlywayContainer {

    private final Flyway flyway;

    private final boolean baselineAtStart;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final boolean repairAtStart;

    private final boolean validateAtStart;
    private final String dataSourceName;
    private final String name;
    private final boolean hasMigrations;
    private final boolean createPossible;
    private final String id;

    public FlywayContainer(Flyway flyway, boolean baselineAtStart, boolean cleanAtStart, boolean migrateAtStart,
            boolean repairAtStart, boolean validateAtStart,
            String dataSourceName, String name, String id, boolean hasMigrations, boolean createPossible) {
        this.flyway = flyway;
        this.baselineAtStart = baselineAtStart;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.repairAtStart = repairAtStart;
        this.validateAtStart = validateAtStart;
        this.dataSourceName = dataSourceName;
        this.name = name;
        this.hasMigrations = hasMigrations;
        this.createPossible = createPossible;
        this.id = id;
    }

    public Flyway getFlyway() {
        return flyway;
    }

    public boolean isBaselineAtStart() {
        return baselineAtStart;
    }

    public boolean isCleanAtStart() {
        return cleanAtStart;
    }

    public boolean isMigrateAtStart() {
        return migrateAtStart;
    }

    public boolean isRepairAtStart() {
        return repairAtStart;
    }

    public boolean isValidateAtStart() {
        return validateAtStart;
    }

    public String getName() {
        return name;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public boolean isHasMigrations() {
        return hasMigrations;
    }

    public boolean isCreatePossible() {
        return createPossible;
    }

    public String getId() {
        return this.id;
    }

    public void doStartActions() {
        if (isCleanAtStart()) {
            getFlyway().clean();
        }
        if (isValidateAtStart()) {
            getFlyway().validate();
        }
        if (isBaselineAtStart()) {
            new FlywayExecutor(getFlyway().getConfiguration())
                    .execute(new BaselineCommand(getFlyway()), true, null);
        }
        if (isRepairAtStart()) {
            getFlyway().repair();
        }
        if (isMigrateAtStart()) {
            getFlyway().migrate();
        }
    }

    static class BaselineCommand implements FlywayExecutor.Command<BaselineResult> {
        BaselineCommand(Flyway flyway) {
            this.flyway = flyway;
        }

        final Flyway flyway;

        @Override
        public BaselineResult execute(CompositeMigrationResolver cmr, SchemaHistory schemaHistory, Database d,
                Schema defaultSchema, Schema[] s, CallbackExecutor ce, StatementInterceptor si) {
            if (!schemaHistory.exists()) {
                return flyway.baseline();
            }
            return null;
        }
    }
}
