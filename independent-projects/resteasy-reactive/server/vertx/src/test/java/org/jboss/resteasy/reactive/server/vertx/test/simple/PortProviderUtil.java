package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.net.URI;

/**
 * Utility class that provides a port number for the Resteasy embedded container.
 */
public class PortProviderUtil {

    /**
     * Create a URI for the provided path, using the configured port
     *
     * @param path the request path
     * @return a full URI
     */
    public static URI createURI(String path) {
        return URI.create(generateURL(path));
    }

    /**
     * Generate a base URL incorporating the configured port.
     *
     * @return a full URL
     */
    public static String generateBaseUrl() {
        return generateURL("");
    }

    /**
     * Generate a URL with port, hostname
     *
     * @param path the path
     * @return a full URL
     */
    public static String generateURL(String path) {
        return "http://localhost:8080" + path;
    }

    public static String generateURL(String path, String ignore) {
        return generateURL(path);
    }

    /**
     * Get port.
     *
     * @return The port number
     */
    public static int getPort() {
        return 8080;
    }

    /**
     * Get host IP.
     *
     * @return The host IP
     */
    public static String getHost() {
        return "localhost";
    }
}
