package io.quarkus.hibernate.orm.data_management;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Schema management strategies to run init script tests with,
 * as scenarios of a {@link org.junit.jupiter.params.ParameterizedClass}.
 */
public enum SchemaManagementScenario {

    /**
     * The default in tests: Dev Services provides the database, so Hibernate ORM drops and creates the schema,
     * and thus executes the schema init script.
     */
    DEFAULT(true) {
        @Override
        public void configure(QuarkusExtensionTest runner, String databaseName) {
        }
    },
    /**
     * Hibernate ORM updates the schema, and thus does not execute the schema init script.
     */
    UPDATE(false) {
        @Override
        public void configure(QuarkusExtensionTest runner, String databaseName) {
            runner.overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "update");
        }
    },
    /**
     * The schema is managed by another tool, so Hibernate ORM does not execute the schema init script.
     */
    NONE(false) {
        @Override
        public void configure(QuarkusExtensionTest runner, String databaseName) {
            runner.overrideConfigKey("quarkus.datasource.db-kind", "h2")
                    .overrideConfigKey("quarkus.datasource.jdbc.url", PreexistingSchemaH2Database.jdbcUrl(databaseName))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");
        }
    };

    public final boolean executesSchemaInitScript;

    SchemaManagementScenario(boolean executesSchemaInitScript) {
        this.executesSchemaInitScript = executesSchemaInitScript;
    }

    /**
     * @param databaseName The name of the H2 database holding the pre-existing schema, if this scenario needs one.
     */
    public abstract void configure(QuarkusExtensionTest runner, String databaseName);
}
