package io.quarkus.test.common;

import static io.quarkus.test.common.http.TestHTTPResourceManager.*;
import static io.quarkus.test.common.http.TestHTTPResourceManager.testUrlSsl;

import java.net.URI;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;

public record ListeningAddress(Integer port, String protocol) {

    public boolean isSsl() {
        return "https".equals(protocol);
    }

    public void register(ValueRegistry valueRegistry, Config config) {
        valueRegistry.register(isSsl() ? HTTPS_PORT : HTTP_PORT, port);
        valueRegistry.register(isSsl() ? HTTPS_TEST_PORT : HTTP_TEST_PORT, port);
        valueRegistry.register(LOCAL_BASE_URI,
                URI.create(isSsl() ? testUrlSsl(valueRegistry, config) : testUrl(valueRegistry, config)));
    }

    public void registerManagement(ValueRegistry valueRegistry, Config config) {
        valueRegistry.register(MANAGEMENT_PORT, port);
        valueRegistry.register(MANAGEMENT_TEST_PORT, port);
        valueRegistry.register(LOCAL_MANAGEMENT_BASE_URI,
                URI.create(isSsl() ? testManagementUrlSsl(valueRegistry, config)
                        : testManagementUrl(valueRegistry, config)));
    }

    // Compatibility with Config and io.quarkus.vertx.http.HttpServer
    public static final RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    public static final RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");
    public static final RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
    public static final RuntimeKey<Integer> HTTPS_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-ssl-port");
    public static final RuntimeKey<URI> LOCAL_BASE_URI = RuntimeKey.key("quarkus.http.local-base-uri");
    public static final RuntimeKey<Optional<ListeningAddress>> LISTENING_ADDRESS = RuntimeKey
            .key("quarkus.http.listening-address");

    // Compatibility with Config and io.quarkus.vertx.http.HttpServer (management interface)
    public static final RuntimeKey<Integer> MANAGEMENT_PORT = RuntimeKey.intKey("quarkus.management.port");
    public static final RuntimeKey<Integer> MANAGEMENT_TEST_PORT = RuntimeKey.intKey("quarkus.management.test-port");
    public static final RuntimeKey<URI> LOCAL_MANAGEMENT_BASE_URI = RuntimeKey.key("quarkus.management.local-base-uri");
    public static final RuntimeKey<Optional<ListeningAddress>> MANAGEMENT_LISTENING_ADDRESS = RuntimeKey
            .key("quarkus.management.listening-address");
}
