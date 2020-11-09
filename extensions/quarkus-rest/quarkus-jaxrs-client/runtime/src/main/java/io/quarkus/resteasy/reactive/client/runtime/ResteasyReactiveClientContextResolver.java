package io.quarkus.resteasy.reactive.client.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.client.ClientContext;
import org.jboss.resteasy.reactive.client.ClientContextResolver;
import org.jboss.resteasy.reactive.client.ClientProxies;
import org.jboss.resteasy.reactive.common.runtime.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.runtime.core.Serialisers;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;

public class ResteasyReactiveClientContextResolver implements ClientContextResolver {
    @Override
    public ClientContext resolve(ClassLoader classLoader) {
        return new ClientContext() {
            @Override
            public Serialisers getSerialisers() {
                return ResteasyReactiveClientRecorder.getSerialisers();
            }

            @Override
            public GenericTypeMapping getGenericTypeMapping() {
                return ResteasyReactiveClientRecorder.getGenericTypeMapping();
            }

            @Override
            public Supplier<Vertx> getVertx() {
                return VertxCoreRecorder.getVertx();
            }

            @Override
            public ClientProxies getClientProxies() {
                return ResteasyReactiveClientRecorder.getClientProxies();
            }
        };
    }
}
