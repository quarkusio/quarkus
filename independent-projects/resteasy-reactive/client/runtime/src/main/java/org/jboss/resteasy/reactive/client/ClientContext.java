package org.jboss.resteasy.reactive.client;

import io.vertx.core.Vertx;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.runtime.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.runtime.core.Serialisers;

public interface ClientContext {
    Serialisers getSerialisers();

    GenericTypeMapping getGenericTypeMapping();

    Supplier<Vertx> getVertx();

    ClientProxies getClientProxies();
}
