package io.quarkus.infinispan.client.substitutions;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.commons.marshall.Marshaller;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This class is here solely to remove the mbeanObjectName field and all methods that would access it
 * 
 * @author William Burns
 */
@TargetClass(RemoteCacheManager.class)
public final class SubstituteRemoteCacheManager {
    @Alias
    private Marshaller marshaller;
    @Alias
    private Configuration configuration;

    @Substitute
    private void initRemoteCache(RemoteCacheImpl remoteCache, OperationsFactory operationsFactory) {
        // Invoke the init method that doesn't have the JMX ObjectName argument
        remoteCache.init(marshaller, operationsFactory, configuration.keySizeEstimate(),
                configuration.valueSizeEstimate(), configuration.batchSize());
    }

    @Substitute
    private void registerMBean() {

    }

    @Substitute
    private void unregisterMBean() {

    }
}
