package io.quarkus.infinispan.client.runtime.graal;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.infinispan.client.hotrod.impl.transport.netty.OperationDispatcher;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

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
    @Alias
    private OperationDispatcher dispatcher;

    @Substitute
    private void initRemoteCache(InternalRemoteCache<?, ?> remoteCache) {
        // Invoke the init method that doesn't have the JMX ObjectName argument
        remoteCache.init(configuration, dispatcher);
    }

    @Substitute
    private void registerMBean() {
    }

    @Substitute
    private void unregisterMBean() {
    }

    @Substitute
    private void registerProtoStreamMarshaller() {
    }

    @Substitute
    private void initializeProtoStreamMarshaller(ProtoStreamMarshaller protoMarshaller) {
        SerializationContext ctx = protoMarshaller.getSerializationContext();

        // Register the configured schemas.
        for (SerializationContextInitializer sci : configuration.getContextInitializers()) {
            sci.registerSchema(ctx);
            sci.registerMarshallers(ctx);
        }
    }
}
