package io.quarkus.vertx.http;

import io.quarkus.registry.RuntimeInfoProvider;
import io.quarkus.registry.ValueRegistry;

/**
 * Registers the {@link HttpServer} with {@link ValueRegistry}.
 * <p>
 * The discovery is only enabled for integration tests. In normal mode, the registration is done with a CDI bean to
 * avoid the ServiceLoader.
 *
 * @see io.quarkus.vertx.http.HttpServerProducer
 */
public class HttpServerRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry) {
        valueRegistry.registerInfo(HttpServer.HTTP_SERVER, HttpServer.INFO);
    }
}
