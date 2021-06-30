package io.quarkus.resteasy.reactive.server.common.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public interface EndpointInvokerFactory {

    Supplier<EndpointInvoker> invoker(String baseName);
}
