package io.quarkus.jaxrs.client.reactive.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.client.impl.DefaultClientContext;
import org.jboss.resteasy.reactive.client.spi.ClientContext;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;

public class JaxrsClientReactiveClientContextResolver implements ClientContextResolver {
    @Override
    public ClientContext resolve(ClassLoader classLoader) {
        return new ClientContext() {
            @Override
            public Serialisers getSerialisers() {
                Serialisers serialisers = JaxrsClientReactiveRecorder.getSerialisers();
                if (serialisers == null) {
                    return DefaultClientContext.INSTANCE.getSerialisers();
                }
                return serialisers;
            }

            @Override
            public GenericTypeMapping getGenericTypeMapping() {
                GenericTypeMapping genericTypeMapping = JaxrsClientReactiveRecorder.getGenericTypeMapping();
                if (genericTypeMapping == null) {
                    return DefaultClientContext.INSTANCE.getGenericTypeMapping();
                }
                return genericTypeMapping;
            }

            @Override
            public Supplier<Vertx> getVertx() {
                Supplier<Vertx> vertx = VertxCoreRecorder.getVertx();
                if (vertx == null) {
                    return DefaultClientContext.INSTANCE.getVertx();
                }
                return vertx;
            }

            @Override
            public ClientProxies getClientProxies() {
                ClientProxies clientProxies = JaxrsClientReactiveRecorder.getClientProxies();
                if (clientProxies == null) {
                    return DefaultClientContext.INSTANCE.getClientProxies();
                }
                return clientProxies;
            }
        };
    }
}
