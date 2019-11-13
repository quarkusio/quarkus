package io.quarkus.quartz.runtime.graal;

import java.rmi.RemoteException;

import org.quartz.core.RemotableQuartzScheduler;

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

final class QuartzSubstitutions {
}
