package io.quarkus.jdbc.derby.runtime;

import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.DERBY)
public class DerbyAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

}
