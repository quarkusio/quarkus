package io.quarkus.jdbc.runtime.runtime;

/**
 * Constants for the runtime-resolved JDBC driver support.
 */
public final class RuntimeJdbc {

    /**
     * The database kind of datasources whose JDBC driver is resolved at runtime from the
     * {@code quarkus.datasource[."datasource-name"].jdbc-runtime.driver} configuration property.
     */
    public static final String DB_KIND = "runtime";

    private RuntimeJdbc() {
    }
}
