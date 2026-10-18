package io.quarkus.runtime;

import java.net.BindException;

/**
 * An exception that is meant to stand in for {@link BindException} and provide information
 * about the target which caused the bind exception.
 */
public class QuarkusBindException extends BindException {

    private final String host;
    private final int port;

    public QuarkusBindException(String host, int port, BindException e) {
        super(createMessage(host, port) + ": " + e.getMessage());
        this.host = host;
        this.port = port;
    }

    private static String createMessage(String host, int port) {
        // in all these cases, the only thing that can cause a bind exception is the port being in use
        if (isKnownHost(host)) {
            return "Port already bound: " + port;
        } else {
            return "Unable to bind to host: " + host + " and port: " + port;
        }
    }

    public static boolean isKnownHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
