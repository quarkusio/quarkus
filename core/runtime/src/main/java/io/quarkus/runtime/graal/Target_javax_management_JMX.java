package io.quarkus.runtime.graal;

import java.util.function.BooleanSupplier;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = JMX.class, onlyWith = Target_javax_management_JMX.JmxServerNotIncluded.class)
final class Target_javax_management_JMX {

    @Substitute
    private static <T> T createProxy(MBeanServerConnection connection,
            ObjectName objectName,
            Class<T> interfaceClass,
            boolean notificationEmitter,
            boolean isMXBean) {
        throw new IllegalStateException("Not Implemented in native mode");
    }

    static final class JmxServerNotIncluded implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            String monitoringProperty = System.getProperty("quarkus.native.monitoring");
            if (monitoringProperty != null) {
                return !monitoringProperty.contains("jmxserver");
            }
            return true;
        }
    }
}
