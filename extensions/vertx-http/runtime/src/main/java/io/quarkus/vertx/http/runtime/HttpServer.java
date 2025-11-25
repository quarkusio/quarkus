package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.RuntimeValues;
import io.quarkus.runtime.RuntimeValues.RuntimeKey;

public class HttpServer {
    private final static HttpServer INSTANCE = new HttpServer();

    public static final RuntimeKey<Integer> HTTP_PORT = RuntimeKey.intKey("quarkus.http.port");
    public static final RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");
    public static final RuntimeKey<Integer> HTTPS_PORT = RuntimeKey.intKey("quarkus.http.ssl-port");
    public static final RuntimeKey<Integer> HTTPS_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-ssl-port");
    public static final RuntimeKey<Integer> MANAGEMENT_PORT = RuntimeKey.intKey("quarkus.management.port");
    public static final RuntimeKey<Integer> MANAGEMENT_TEST_PORT = RuntimeKey.intKey("quarkus.management.test-port");

    private HttpServer() {
    }

    /**
     * Return the http port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the http port.
     */
    public int getPort() {
        // TODO - What if this method gets called before being set? Should this be an event instead?
        return RuntimeValues.get(HTTP_PORT, -1);
    }

    static void setPort(int port) {
        RuntimeValues.register(HTTP_PORT, port);
    }

    static void setTestPort(int testPort) {
        RuntimeValues.register(HTTP_TEST_PORT, testPort);
    }

    /**
     * Return the https port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the https port.
     */
    public int getSecurePort() {
        return RuntimeValues.get(HTTPS_PORT, -1);
    }

    static void setSecurePort(int port) {
        RuntimeValues.register(HTTPS_PORT, port);
    }

    static void setSecureTestPort(int testPort) {
        RuntimeValues.register(HTTPS_TEST_PORT, testPort);
    }

    /**
     * Return the management http port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the management http port.
     */
    public int getManagementPort() {
        return RuntimeValues.get(MANAGEMENT_PORT, -1);
    }

    static void setManagementPort(int managementPort) {
        RuntimeValues.register(MANAGEMENT_PORT, managementPort);
    }

    static void setManagementTestPort(int testPort) {
        RuntimeValues.register(MANAGEMENT_TEST_PORT, testPort);
    }

    public static HttpServer instance() {
        return INSTANCE;
    }
}
