package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.Vertx;
import java.util.Collections;
import java.util.function.Supplier;
import javax.ws.rs.RuntimeType;
import org.jboss.resteasy.reactive.client.spi.ClientContext;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;

public class DefaultClientContext implements ClientContext {

    public static DefaultClientContext INSTANCE = new DefaultClientContext();
    public static final ClientContextResolver RESOLVER = new ClientContextResolver() {
        @Override
        public ClientContext resolve(ClassLoader classLoader) {
            return INSTANCE;
        }
    };

    final GenericTypeMapping genericTypeMapping;
    final ClientSerialisers serialisers;
    final ClientProxies clientProxies;

    public DefaultClientContext() {
        serialisers = new ClientSerialisers();
        serialisers.registerBuiltins(RuntimeType.CLIENT);
        clientProxies = new ClientProxies(Collections.emptyMap(), Collections.emptyMap());
        genericTypeMapping = new GenericTypeMapping();
    }

    @Override
    public ClientSerialisers getSerialisers() {
        return serialisers;
    }

    @Override
    public GenericTypeMapping getGenericTypeMapping() {
        return genericTypeMapping;
    }

    @Override
    public Supplier<Vertx> getVertx() {
        return null;
    }

    @Override
    public ClientProxies getClientProxies() {
        return clientProxies;
    }
}
