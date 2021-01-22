package io.quarkus.quartz.runtime.graal;

import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.util.function.BooleanSupplier;

import org.graalvm.home.Version;
import org.quartz.core.RemotableQuartzScheduler;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.quartz.core.QuartzScheduler")
final class Target_org_quartz_core_QuartzScheduler {

    @Substitute
    private void bind() throws RemoteException {
    }

    @Substitute
    private void unBind() throws RemoteException {
    }

    @Substitute
    private void registerJMX() throws Exception {
    }

    @Substitute
    private void unregisterJMX() throws Exception {
    }
}

@TargetClass(className = "org.quartz.impl.RemoteScheduler")
final class Target_org_quartz_impl_RemoteScheduler {

    @Substitute
    protected RemotableQuartzScheduler getRemoteScheduler() {
        return null;
    }

}

@TargetClass(value = StdJDBCDelegate.class, onlyWith = GraalVMLessThan21.class)
@Deprecated
/**
 * This was only added to avoid Object serialization which is not supported by GraalVM version less than 21.
 * - see https://github.com/oracle/graal/issues/460
 * - https://www.graalvm.org/release-notes/21_0/
 *
 * The substitutions is kept around for backward compatibility reason and it will be removed in the future.
 */
final class Target_org_quartz_impl_jdbc_jobstore_StdJDBCDelegate {

    /**
     * Activate the usage of {@link java.util.Properties} to avoid Object serialization
     * 
     * @return true
     */
    @Substitute
    protected boolean canUseProperties() {
        return true;
    }

    @Substitute
    protected ByteArrayOutputStream serializeObject(Object obj) {
        throw new IllegalStateException("Object serialization not supported."); // should not reach here
    }

    @Substitute
    protected Object getObjectFromBlob(ResultSet rs, String colName) {
        throw new IllegalStateException("Object serialization not supported."); // should not reach here
    }
}

final class GraalVMLessThan21 implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Version.getCurrent().compareTo(21) < 0;
    }
}

final class QuartzSubstitutions {
}
