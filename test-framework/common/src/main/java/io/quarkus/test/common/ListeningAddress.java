package io.quarkus.test.common;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeKey;

public record ListeningAddress(Integer port, String protocol) {

    public boolean isSsl() {
        return "https".equals(protocol);
    }

    public void register(ValueRegistry valueRegistry) {
        valueRegistry.register(PORT, port);
        valueRegistry.register(PROTOCOL, protocol);
        valueRegistry.register(isSsl() ? HTTPS_PORT : HTTP_PORT, port);
    }

    public static final RuntimeKey<Integer> PORT = RuntimeKey.intKey("port");
    public static final RuntimeKey<String> PROTOCOL = RuntimeKey.key("protocol");
    // Compatibility with Config and io.quarkus.vertx.http.HttpServer
    public static final RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    public static final RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
}
