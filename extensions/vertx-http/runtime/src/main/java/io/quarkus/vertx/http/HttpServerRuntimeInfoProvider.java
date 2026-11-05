package io.quarkus.vertx.http;

import io.quarkus.runtime.Address;
import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.ValueRegistry;

/**
 * Registers the {@link HttpServer} with {@link ValueRegistry}, and the {@link Address} of each listener, derived from
 * it, under {@link Address#key(String)} so that {@code quarkus://vertx-http} and {@code quarkus://vertx-management}
 * resolve without CDI.
 * <p>
 * In normal mode, the {@link HttpServer} is also registered with a CDI Bean to support injection.
 */
public class HttpServerRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource runtimeSource) {
        valueRegistry.registerInfo(HttpServer.HTTP_SERVER, HttpServer.INFO);
        valueRegistry.registerInfo(HttpServer.ADDRESS_VERTX_HTTP,
                registry -> HttpServer.INFO.get(registry).getAddress().orElse(null));
        valueRegistry.registerInfo(HttpServer.ADDRESS_VERTX_MANAGEMENT,
                registry -> HttpServer.INFO.get(registry).getManagementAddress().orElse(null));
    }
}
