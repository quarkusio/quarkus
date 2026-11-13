package io.quarkus.redis.lettuce.deployment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Picks free host ports for the containers that must publish a port on the same host port (see
 * {@link RedisClusterTestResource} and {@link RedisSentinelTestResource}).
 */
final class TestPorts {

    /**
     * The ports handed out so far, never handed out again: the containers of the module run side by side (the
     * resources are global), and each picks its ports in its own call.
     */
    private static final Set<Integer> RESERVED = new HashSet<>();

    private TestPorts() {
    }

    /**
     * Picks {@code count} distinct free ports between 20000 and 55000: above the well-known range, and below 55536
     * so that a Redis cluster bus port (the port plus 10000) still fits. A port picked by an earlier call is never
     * picked again, so two containers cannot be given the same port.
     * <p>
     * The probe binds all interfaces <em>without</em> address reuse: with {@code SO_REUSEADDR}, the JDK default, a
     * port another process has bound on {@code 127.0.0.1} only (an IDE, say) still binds on {@code 0.0.0.0} and
     * would be reported free, although every connection to {@code 127.0.0.1} on it reaches that process. Every probe
     * socket stays open until all the ports are picked, so the same port is not picked twice within a call.
     */
    static synchronized List<Integer> free(int count) {
        Random random = new Random();
        List<ServerSocket> sockets = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();
        try {
            while (ports.size() < count) {
                int candidate = 20000 + random.nextInt(35000);
                if (RESERVED.contains(candidate)) {
                    continue;
                }
                ServerSocket socket = new ServerSocket();
                try {
                    socket.setReuseAddress(false);
                    socket.bind(new InetSocketAddress(candidate));
                    sockets.add(socket);
                    ports.add(candidate);
                } catch (IOException taken) {
                    socket.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to pick free ports", e);
        } finally {
            for (ServerSocket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
        RESERVED.addAll(ports);
        return List.copyOf(ports);
    }
}
