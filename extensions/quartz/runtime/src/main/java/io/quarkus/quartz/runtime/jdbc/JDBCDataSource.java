package io.quarkus.quartz.runtime.jdbc;

/**
 * A simplified representation of JDBC data source which we need in order to defer resolving a data source until runtime.
 *
 */
public record JDBCDataSource(String name, boolean isDefault, String dbKind) {

}
