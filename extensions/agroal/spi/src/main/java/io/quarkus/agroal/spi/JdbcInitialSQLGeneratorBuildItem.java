package io.quarkus.agroal.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

public final class JdbcInitialSQLGeneratorBuildItem extends MultiBuildItem {

    final String databaseName;
    final Supplier<String> sqlSupplier;

    public JdbcInitialSQLGeneratorBuildItem(String databaseName, Supplier<String> sqlSupplier) {
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
