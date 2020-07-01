package io.quarkus.jdbc.db2.runtime;

import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.DB2)
public class DB2AgroalConnectionConfigurer implements AgroalConnectionConfigurer {
}
