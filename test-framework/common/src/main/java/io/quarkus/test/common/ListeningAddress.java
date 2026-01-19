package io.quarkus.test.common;

import java.net.URI;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeKey;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

public record ListeningAddress(Integer port, String protocol) {

    public boolean isSsl() {
        return "https".equals(protocol);
    }

    public void register(ValueRegistry valueRegistry, SmallRyeConfig config) {
        valueRegistry.register(isSsl() ? HTTPS_PORT : HTTP_PORT, port);
        valueRegistry.register(isSsl() ? HTTPS_TEST_PORT : HTTP_TEST_PORT, port);
        valueRegistry.register(LOCAL_BASE_URI, URI.create(TestHTTPResourceManager.testUrl(valueRegistry, config)));
    }

    // Compatibility with Config and io.quarkus.vertx.http.HttpServer
    public static final RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    public static final RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");
    public static final RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
    public static final RuntimeKey<Integer> HTTPS_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-ssl-port");
    public static final RuntimeKey<URI> LOCAL_BASE_URI = RuntimeKey.key("quarkus.http.local-base-uri");
}
