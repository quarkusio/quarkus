package org.jboss.shamrock.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.internal.logging.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.mariadb.jdbc.internal.util.pool.Pool.class)
public final class Pool_disable_JMX {

    @Alias
    private static Logger logger = null;

    @Substitute
    private void registerJmx() throws Exception {
        logger.warn("MariaDB driver can't register to JMX MBeans in GraalVM: request to register ignored");
    }

    @Substitute
    private void unRegisterJmx() throws Exception {
        //no-op
    }

}
