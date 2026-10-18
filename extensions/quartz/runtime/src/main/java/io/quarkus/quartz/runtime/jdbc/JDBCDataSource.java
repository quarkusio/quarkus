package io.quarkus.quartz.runtime.jdbc;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A simplified representation of JDBC data source which we need in order to defer resolving a data source until runtime.
 *
 */
public class JDBCDataSource {

    private final String name;
    private final boolean isDefault;
    private final String dbKind;

    @RecordableConstructor
    public JDBCDataSource(String name, boolean isDefault, String dbKind) {
        this.name = name;
        this.isDefault = isDefault;
        this.dbKind = dbKind;
    }

    public String getName() {
        return name;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public String getDbKind() {
        return dbKind;
    }
}
