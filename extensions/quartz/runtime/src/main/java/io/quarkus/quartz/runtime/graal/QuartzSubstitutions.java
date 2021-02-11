package io.quarkus.quartz.runtime.graal;

import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;

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

@TargetClass(StdJDBCDelegate.class)
final class Target_org_quartz_impl_jdbc_jobstore_StdJDBCDelegate {

    /**
     * Activate the usage of {@link java.util.Properties} to avoid Object serialization
     * which is not supported by GraalVM - see https://github.com/oracle/graal/issues/460
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

final class QuartzSubstitutions {
}
