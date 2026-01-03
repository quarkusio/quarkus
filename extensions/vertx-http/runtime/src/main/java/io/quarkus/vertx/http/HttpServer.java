package io.quarkus.vertx.http;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.registry.ValueRegistry.RuntimeKey;

/**
 * Represent the actual runtime values of the Quarkus HTTP Server.
 */
// TODO - Ideally we should store the SocketAddress, but Vertx only returns the port. Look into Vert.x to change it.
public interface HttpServer {
    RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");
    RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
    RuntimeKey<Integer> HTTPS_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-ssl-port");
    RuntimeKey<Integer> MANAGEMENT_PORT = RuntimeKey.intKey("quarkus.management.port");
    RuntimeKey<Integer> MANAGEMENT_TEST_PORT = RuntimeKey.intKey("quarkus.management.test-port");

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
            };
        }
    };
}
