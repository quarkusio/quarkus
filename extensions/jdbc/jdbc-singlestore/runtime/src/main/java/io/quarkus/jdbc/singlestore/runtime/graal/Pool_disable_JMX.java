package io.quarkus.jdbc.singlestore.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.singlestore.jdbc.util.log.Logger;

@TargetClass(com.singlestore.jdbc.pool.Pool.class)
public final class Pool_disable_JMX {

    @Alias
    private Logger logger = null;

    @Substitute
    private void registerJmx() throws Exception {
        logger.warn("Singlestore driver can't register to JMX MBeans in GraalVM: request to register ignored");
    }

    @Substitute
    private void unRegisterJmx() throws Exception {
        //no-op
    }

}
