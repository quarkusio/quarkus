package io.quarkus.jdbc.oracle.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "oracle.jdbc.datasource.impl.OracleDataSource")
public final class OracleDatasourceSubstitutions {

    @Substitute
    public static void unregisterMBean() {
        //No-op
    }

    @Substitute
    public static void registerMBean() {
        //No-op
    }

}
