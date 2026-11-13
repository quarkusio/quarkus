package io.quarkus.vertx.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.Address;
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
    RuntimeKey<URI> LOCAL_MANAGEMENT_BASE_URI = RuntimeKey.key("quarkus.management.local-base-uri");

    RuntimeKey<InetSocketAddress> HTTP_SOCKET = RuntimeKey.key("quarkus.http.socket-address", InetSocketAddress.class);
    RuntimeKey<InetSocketAddress> HTTPS_SOCKET = RuntimeKey.key("quarkus.http.ssl-socket-address", InetSocketAddress.class);
    RuntimeKey<UnixDomainSocketAddress> HTTP_DOMAIN_SOCKET = RuntimeKey.key("quarkus.http.domain-socket-address",
            UnixDomainSocketAddress.class);
    RuntimeKey<InetSocketAddress> MANAGEMENT_SOCKET = RuntimeKey.key("quarkus.management.socket-address",
            InetSocketAddress.class);
    RuntimeKey<UnixDomainSocketAddress> MANAGEMENT_DOMAIN_SOCKET = RuntimeKey.key("quarkus.management.domain-socket-address",
            UnixDomainSocketAddress.class);

    String VERTX_HTTP = "vertx-http";
    String VERTX_HTTPS = "vertx-https";
    String VERTX_MANAGEMENT = "vertx-management";

    RuntimeKey<Address> ADDRESS_VERTX_HTTP = Address.key(VERTX_HTTP);
    RuntimeKey<Address> ADDRESS_VERTX_HTTPS = Address.key(VERTX_HTTPS);
    RuntimeKey<Address> ADDRESS_VERTX_MANAGEMENT = Address.key(VERTX_MANAGEMENT);

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
     * Return the local base URI that Quarkus is serving on, including the main HTTP/HTTPS port and the root path.
     * This is the URI view of the HTTP server: HTTPS when TLS is on and insecure requests are not enabled, HTTP
     * otherwise.
     *
     * @return the local base URI that Quarkus is serving on.
     */
    URI getLocalBaseUri();

    /**
     * Return the local base URI that Quarkus management interface is serving on, including the management port and
     * the management root path.
     *
     * @return the local base URI that Quarkus management interface is serving on, or an empty {@link Optional} if the
     *         management interface is not enabled.
     */
    Optional<URI> getManagementBaseUri();

    /**
     * The {@link Address} of the HTTP port: the sockets the server bound for plain HTTP, that is {@link #HTTP_SOCKET}
     * and {@link #HTTP_DOMAIN_SOCKET}, whichever are enabled.
     *
     * @return the address, or empty when Quarkus is not listening for plain HTTP
     */
    Optional<Address> getAddress();

    /**
     * The {@link Address} of the HTTPS port: {@link #HTTPS_SOCKET}.
     *
     * @return the address, or empty when Quarkus is not listening on the https port
     */
    Optional<Address> getSecureAddress();

    /**
     * The {@link Address} of the management interface: {@link #MANAGEMENT_SOCKET}, with the scheme of
     * {@link #getManagementBaseUri()}, and {@link #MANAGEMENT_DOMAIN_SOCKET}, whichever are enabled. The domain socket
     * speaks plain HTTP, so it is left out when the management port is HTTPS.
     *
     * @return the address, or empty when the management interface is not enabled
     */
    Optional<Address> getManagementAddress();

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

                @Override
                public Optional<URI> getManagementBaseUri() {
                    return Optional.ofNullable(valueRegistry.getOrDefault(LOCAL_MANAGEMENT_BASE_URI, null));
                }

                @Override
                public Optional<Address> getAddress() {
                    return address("http", List.of(HTTP_DOMAIN_SOCKET, HTTP_SOCKET));
                }

                @Override
                public Optional<Address> getSecureAddress() {
                    return address("https", List.of(HTTPS_SOCKET));
                }

                @Override
                public Optional<Address> getManagementAddress() {
                    // no base URI means no management port, so only the domain socket can be bound
                    String protocol = getManagementBaseUri().map(URI::getScheme).orElse("http");
                    return address(protocol, "https".equals(protocol)
                            ? List.of(MANAGEMENT_SOCKET)
                            : List.of(MANAGEMENT_DOMAIN_SOCKET, MANAGEMENT_SOCKET));
                }

                private Optional<Address> address(String protocol, List<RuntimeKey<? extends SocketAddress>> keys) {
                    List<SocketAddress> sockets = new ArrayList<>(keys.size());
                    for (RuntimeKey<? extends SocketAddress> key : keys) {
                        SocketAddress socket = valueRegistry.getOrDefault(key, null);
                        if (socket != null) {
                            sockets.add(socket);
                        }
                    }
                    return sockets.isEmpty() ? Optional.empty() : Optional.of(Address.of(protocol, sockets));
                }
            };
        }
    };
}
