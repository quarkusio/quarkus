package io.quarkus.jdbc.oracle.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "oracle.jdbc.driver.OracleDriver")
public final class OracleDriverSubstitutions {

    @Substitute
    static void unRegisterMBeans() {
        //No-op
    }

    @Substitute
    static void registerMBeans() {
        //No-op
    }

}
