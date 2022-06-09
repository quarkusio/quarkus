package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An Hibernate Dialect associated with a database kind.
 */
public final class DatabaseKindDialectBuildItem extends MultiBuildItem {
    private final String dbKind;
    private final String dialect;

    public DatabaseKindDialectBuildItem(String dbKind, String dialect) {
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
