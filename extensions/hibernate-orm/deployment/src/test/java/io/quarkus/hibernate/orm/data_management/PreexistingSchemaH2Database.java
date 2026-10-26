package io.quarkus.hibernate.orm.data_management;

import io.quarkus.hibernate.orm.MyEntity;

/**
 * Helper to test schema management strategies that do not create the schema:
 * builds an H2 JDBC URL whose {@code INIT} clause creates the schema of {@link MyEntity}
 * before Hibernate ORM starts, as another tool (Flyway, Liquibase, ...) would.
 */
public final class PreexistingSchemaH2Database {

    private PreexistingSchemaH2Database() {
    }

    public static String jdbcUrl(String databaseName) {
        return "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1"
                + ";INIT=CREATE TABLE IF NOT EXISTS MyEntity(id BIGINT NOT NULL PRIMARY KEY, name VARCHAR(50))"
                + "\\;CREATE SEQUENCE IF NOT EXISTS myEntitySeq START WITH 1 INCREMENT BY 50";
    }
}
