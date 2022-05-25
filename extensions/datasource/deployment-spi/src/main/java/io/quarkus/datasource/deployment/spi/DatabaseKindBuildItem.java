package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class DatabaseKindBuildItem extends MultiBuildItem {
    private final String dbKind;
    private final String dialect;

    public DatabaseKindBuildItem(String dbKind, String dialect) {
        this.dbKind = dbKind;
        this.dialect = dialect;
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getDialect() {
        return dialect;
    }
}
