package org.jboss.resteasy.reactive.server.spi;

import java.util.function.Supplier;

public interface EndpointInvokerFactory {

    Supplier<EndpointInvoker> invoker(String baseName);
}
