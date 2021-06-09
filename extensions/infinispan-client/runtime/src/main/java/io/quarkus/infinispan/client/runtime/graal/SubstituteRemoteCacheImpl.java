package io.quarkus.infinispan.client.runtime.graal;

import javax.management.ObjectName;

import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This class is here solely to remove the mbeanObjectName field and all methods that would access it
 * 
 * @author William Burns
 */
@TargetClass(RemoteCacheImpl.class)
public final class SubstituteRemoteCacheImpl {
    @Delete
    private ObjectName mbeanObjectName;

    @Substitute
    private void registerMBean(ObjectName jmxParent) {
    }

    @Substitute
    private void unregisterMBean() {
    }

    // Sadly this method is public, so technically a user could get a Runtime error if they were referencing
    // it before - but it is the only way to make graal happy
    @Delete
    public void init(OperationsFactory operationsFactory, Configuration configuration, ObjectName jmxParent) {
    }
}
