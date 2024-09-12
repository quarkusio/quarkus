package io.quarkus.runtime.configuration;

import static java.net.InetAddress.getByName;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Services requiring random ports should use {@link RandomPorts} to assign a port during configuration, usually in a
 * {@link io.smallrye.config.ConfigSourceFactory}. This allows the reference of the assigned port in configuration
 * expressions.
 */
public class RandomPorts {
    private static final Map<Integer, Integer> randomPorts = new ConcurrentHashMap<>();

    /**
     * Assigns a random port.
     * <p>
     * The <code>port</code> parameter is used to cache the assignments of random ports, meaning that if a call is
     * executed to assign a random port to <code>-1</code>, every call with <code>port</code> equals to
     * <code>-1</code> returns the same assigned random port.
     *
     * @param host the host the server will bind to.
     * @param port the port number, must be greater than zero.
     * @return the assigned random port.
     */
    public static int get(final String host, final int port) {
        if (port > 0) {
            throw new IllegalArgumentException("port must be greater than zero");
        }
        return randomPorts.computeIfAbsent(port, new Function<Integer, Integer>() {
            @Override
            public Integer apply(final Integer socketAddress) {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(getByName(host), 0), 0);
                    return serverSocket.getLocalPort();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
