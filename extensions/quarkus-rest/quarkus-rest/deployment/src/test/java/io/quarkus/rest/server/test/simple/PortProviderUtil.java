package io.quarkus.rest.server.test.simple;

import java.net.URI;
import java.net.URISyntaxException;

import io.quarkus.test.common.http.TestHTTPResourceManager;

/**
 * Utility class that provides a port number for the Resteasy embedded container.
 */
public class PortProviderUtil {

    /**
     * Create a Resteasy client proxy with an empty base request path.
     *
     * @param clazz the client interface class
     * @return the proxy object
     */
    public static <T> T createProxy(Class<T> clazz, String testName) {
        return createProxy(clazz, "");
    }

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
        return TestHTTPResourceManager.getUri().replace("0.0.0.0", "localhost") + path;
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
        try {
            return new URI(TestHTTPResourceManager.getUri()).getPort();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get host IP.
     *
     * @return The host IP
     */
    public static String getHost() {
        try {
            return new URI(TestHTTPResourceManager.getUri()).getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
