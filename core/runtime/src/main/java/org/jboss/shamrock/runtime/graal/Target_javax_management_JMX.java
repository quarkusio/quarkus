package org.jboss.shamrock.runtime.graal;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(JMX.class)
final class Target_javax_management_JMX {

    @Substitute
    private static <T> T createProxy(MBeanServerConnection connection,
                                     ObjectName objectName,
                                     Class<T> interfaceClass,
                                     boolean notificationEmitter,
                                     boolean isMXBean) {
        throw new IllegalStateException("Not Implemented in Substrate");
    }

}
