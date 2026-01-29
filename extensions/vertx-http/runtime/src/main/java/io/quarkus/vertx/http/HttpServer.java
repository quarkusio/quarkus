package io.quarkus.vertx.http;

import java.net.URI;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

/**
 * Represent the actual runtime values of the Quarkus HTTP Server.
 */
public interface HttpServer {
    RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");
    RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
    RuntimeKey<Integer> HTTPS_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-ssl-port");
    RuntimeKey<Integer> MANAGEMENT_PORT = RuntimeKey.intKey("quarkus.management.port");
    RuntimeKey<Integer> MANAGEMENT_TEST_PORT = RuntimeKey.intKey("quarkus.management.test-port");
    RuntimeKey<URI> LOCAL_BASE_URI = RuntimeKey.key("quarkus.http.local-base-uri");

    RuntimeKey<HttpServer> HTTP_SERVER = RuntimeKey.key(HttpServer.class);

    /**
     * Return the http port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the http port.
     */
    int getPort();

    /**
     * Return the https port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the https port.
     */
    int getSecurePort();

    /**
     * Return the management http port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the management http port.
     */
    int getManagementPort();

    /**
     * Return the local base URI that Quarkus is serving on.
     *
     * @return the local base URI that Quarkus is serving on.
     */
    URI getLocalBaseUri();

    /**
     * The {@link RuntimeInfo} implementation for {@link HttpServer}. Construct instances of {@link HttpServer} with
     * {@link ValueRegistry} values.
     */
    RuntimeInfo<HttpServer> INFO = new RuntimeInfo<>() {
        @Override
        public HttpServer get(ValueRegistry valueRegistry) {
            return new HttpServer() {
                @Override
                public int getPort() {
                    return valueRegistry.getOrDefault(HTTP_PORT, -1);
                }

                @Override
                public int getSecurePort() {
                    return valueRegistry.getOrDefault(HTTPS_PORT, -1);
                }

                @Override
                public int getManagementPort() {
                    return valueRegistry.getOrDefault(MANAGEMENT_PORT, -1);
                }

                @Override
                public URI getLocalBaseUri() {
                    return valueRegistry.get(LOCAL_BASE_URI);
                }
            };
        }
    };
}
