package io.quarkus.vertx.http;

import io.quarkus.registry.RuntimeInfoProvider;
import io.quarkus.registry.ValueRegistry;

/**
 * Registers the {@link HttpServer} with {@link ValueRegistry}.
 * <p>
 * In normal mode, the {@link HttpServer} is also registered with a CDI Bean to support injection.
 */
public class HttpServerRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource runtimeSource) {
        valueRegistry.registerInfo(HttpServer.HTTP_SERVER, HttpServer.INFO);
    }
}
