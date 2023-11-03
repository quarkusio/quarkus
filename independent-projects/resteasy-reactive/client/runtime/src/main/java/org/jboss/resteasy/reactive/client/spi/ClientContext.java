package org.jboss.resteasy.reactive.client.spi;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.vertx.core.Vertx;

public interface ClientContext {
    Serialisers getSerialisers();

    GenericTypeMapping getGenericTypeMapping();

    Supplier<Vertx> getVertx();

    ClientProxies getClientProxies();

    Map<Class<?>, MultipartResponseData> getMultipartResponsesData();
}
