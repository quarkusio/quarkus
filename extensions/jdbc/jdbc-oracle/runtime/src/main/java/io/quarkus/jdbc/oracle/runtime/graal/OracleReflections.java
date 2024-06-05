package io.quarkus.jdbc.oracle.runtime.graal;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * We don't use a build item here as we also need to register all the nested classes and there's no way to do it easily with the
 * build item for now.
 */
@RegisterForReflection(targets = { oracle.jdbc.xa.OracleXADataSource.class,
        oracle.jdbc.datasource.impl.OracleDataSource.class })
public class OracleReflections {

}
