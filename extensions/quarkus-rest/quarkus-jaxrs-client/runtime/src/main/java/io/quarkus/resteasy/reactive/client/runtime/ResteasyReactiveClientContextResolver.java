package io.quarkus.resteasy.reactive.client.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.client.ClientContext;
import org.jboss.resteasy.reactive.client.ClientContextResolver;
import org.jboss.resteasy.reactive.client.ClientProxies;
import org.jboss.resteasy.reactive.client.DefaultClientContext;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;

public class ResteasyReactiveClientContextResolver implements ClientContextResolver {
    @Override
    public ClientContext resolve(ClassLoader classLoader) {
        return new ClientContext() {
            @Override
            public Serialisers getSerialisers() {
                Serialisers serialisers = ResteasyReactiveClientRecorder.getSerialisers();
                if (serialisers == null) {
                    return DefaultClientContext.INSTANCE.getSerialisers();
                }
                return serialisers;
            }

            @Override
            public GenericTypeMapping getGenericTypeMapping() {
                GenericTypeMapping genericTypeMapping = ResteasyReactiveClientRecorder.getGenericTypeMapping();
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
                ClientProxies clientProxies = ResteasyReactiveClientRecorder.getClientProxies();
                if (clientProxies == null) {
                    return DefaultClientContext.INSTANCE.getClientProxies();
                }
                return clientProxies;
            }
        };
    }
}
