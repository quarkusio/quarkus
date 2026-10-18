package io.quarkus.agroal.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers a JDBC generator that produces SQL update scripts for the specified database.
 * <p>
 * The generated SQL updates the database schema so it matches the current model.
 */
public final class JdbcUpdateSQLGeneratorBuildItem extends MultiBuildItem {

    final String databaseName;
    final Supplier<String> sqlSupplier;

    public JdbcUpdateSQLGeneratorBuildItem(String databaseName, Supplier<String> sqlSupplier) {
        this.databaseName = databaseName;
        this.sqlSupplier = sqlSupplier;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public Supplier<String> getSqlSupplier() {
        return sqlSupplier;
    }
}
