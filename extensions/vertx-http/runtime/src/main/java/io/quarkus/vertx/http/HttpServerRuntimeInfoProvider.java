package io.quarkus.vertx.http;

import io.quarkus.runtime.Address;
import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.ValueRegistry;

/**
 * Registers the {@link HttpServer}, and the {@link Address} of each of its ports, with {@link ValueRegistry}.
 * <p>
 * In normal mode, these are also registered with a CDI Bean to support injection.
 */
public class HttpServerRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource runtimeSource) {
        valueRegistry.registerInfo(HttpServer.HTTP_SERVER, HttpServer.INFO);

        valueRegistry.registerInfo(HttpServer.ADDRESS_VERTX_HTTP,
                (ValueRegistry registry) -> HttpServer.INFO.get(registry).getAddress().orElse(null));
        valueRegistry.registerInfo(HttpServer.ADDRESS_VERTX_HTTPS,
                registry -> HttpServer.INFO.get(registry).getSecureAddress().orElse(null));
        valueRegistry.registerInfo(HttpServer.ADDRESS_VERTX_MANAGEMENT,
                registry -> HttpServer.INFO.get(registry).getManagementAddress().orElse(null));
    }
}
