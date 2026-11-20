package io.quarkus.redis.lettuce.runtime.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.lettuce.core.resource.Transports;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

/**
 * Aligns the Netty transport of Lettuce with the one Vert.x selected.
 * <p>
 * Lettuce picks its transport statically, whichever native transport is on the class path (epoll, io_uring or
 * kqueue), and bootstraps that channel type on the event loops it is given. Vert.x selects its transport from the
 * {@code quarkus.vertx.native-transport*} configuration, NIO by default. With a native transport jar on the class
 * path and the two choices apart, the registration of a Lettuce channel on a Vert.x event loop fails with Netty's
 * "incompatible event loop type". So, before Lettuce's transport providers initialize, their switches are set from
 * the transport Vert.x runs on, and the channel class Lettuce then selects is verified against it. Lettuce 8 and
 * Vert.x 5 both use the io_uring transport of Netty 4.2, so io_uring is shared like the others.
 * <p>
 * The switches are system properties. One that is already set to what the Vert.x transport needs is left alone; one
 * set to the opposite cannot work, so it is overridden, with a warning. {@link #restore()} puts back what the
 * switches held, when the client resources are shut down.
 */
public final class LettuceTransport {

    private static final Logger LOGGER = Logger.getLogger(LettuceTransport.class);

    /**
     * What the switches held before {@link #setSwitches(String)} changed them, {@code null} for one that was not set;
     * {@code null} when nothing is to be restored.
     */
    private static Map<String, String> previous;

    private LettuceTransport() {
    }

    /**
     * Sets Lettuce's transport switches from the Vert.x transport and verifies Lettuce's channel class. Called once,
     * before the shared client resources are created, so before Lettuce's transport providers initialize.
     *
     * @throws IllegalStateException when Lettuce's providers were initialized before the alignment and selected
     *         another transport
     */
    public static void align(Vertx vertx) {
        String name = name(((VertxInternal) vertx).transport());
        if (name == null) {
            LOGGER.warnf("Unknown Vert.x transport %s: the Netty transport of Lettuce is left to its own selection",
                    ((VertxInternal) vertx).transport().getClass().getName());
            return;
        }
        setSwitches(name);
        String expected = channelClass(name);
        Class<?> channel = Transports.socketChannelClass();
        if (!channel.getName().equals(expected)) {
            throw new IllegalStateException(String.format("Lettuce selected the Netty channel %s while Vert.x runs on "
                    + "its %s transport, so the Lettuce connections cannot be registered on the Vert.x event loops. "
                    + "Lettuce's transport providers were initialized before the Lettuce backend could align them: "
                    + "if the application uses Lettuce directly before Quarkus starts, set the io.lettuce.core.epoll, "
                    + "io.lettuce.core.kqueue and io.lettuce.core.iouring system properties to match the Vert.x "
                    + "transport", channel.getName(), name));
        }
        LOGGER.infof("Lettuce runs on the %s transport Vert.x selected (%s)", name, channel.getSimpleName());
    }

    /**
     * Sets Lettuce's transport switches for the Vert.x transport of the given name, remembering what they held for
     * {@link #restore()}. A switch already set to the needed value is not written; one set to the opposite is
     * overridden, with a warning.
     */
    static synchronized void setSwitches(String name) {
        Map<String, Boolean> switches = new LinkedHashMap<>();
        switches.put("io.lettuce.core.epoll", "epoll".equals(name));
        switches.put("io.lettuce.core.kqueue", "kqueue".equals(name));
        switches.put("io.lettuce.core.iouring", "io_uring".equals(name));
        Map<String, String> before = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : switches.entrySet()) {
            String property = entry.getKey();
            String needed = entry.getValue().toString();
            String current = System.getProperty(property);
            before.put(property, current);
            if (current == null) {
                System.setProperty(property, needed);
            } else if (Boolean.parseBoolean(current) != entry.getValue()) {
                LOGGER.warnf("The system property %s=%s is overridden with %s: Lettuce runs on the event loops of "
                        + "Vert.x, which uses its %s transport", property, current, needed, name);
                System.setProperty(property, needed);
            }
        }
        if (previous == null) {
            previous = before;
        }
    }

    /**
     * Puts back what Lettuce's transport switches held before they were set. Lettuce's transport providers read them
     * once, when they initialize, so this does not change the transport of a running client.
     */
    public static synchronized void restore() {
        if (previous == null) {
            return;
        }
        for (Map.Entry<String, String> entry : previous.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
        previous = null;
    }

    /**
     * The name of a Vert.x transport, as {@code quarkus.vertx.native-transport-type} spells it ({@code nio},
     * {@code epoll}, {@code kqueue}, {@code io_uring}), or {@code null} for a transport this class does not know.
     */
    static String name(io.vertx.core.spi.transport.Transport transport) {
        return switch (transport.getClass().getSimpleName()) {
            case "NioTransport", "UnixDomainSocketNioTransport" -> "nio";
            case "EpollTransport" -> "epoll";
            case "KQueueTransport" -> "kqueue";
            case "IoUringTransport" -> "io_uring";
            default -> null;
        };
    }

    /**
     * The Netty socket channel class Lettuce selects for a transport.
     */
    static String channelClass(String name) {
        return switch (name) {
            case "nio" -> "io.netty.channel.socket.nio.NioSocketChannel";
            case "epoll" -> "io.netty.channel.epoll.EpollSocketChannel";
            case "kqueue" -> "io.netty.channel.kqueue.KQueueSocketChannel";
            case "io_uring" -> "io.netty.channel.uring.IoUringSocketChannel";
            default -> throw new IllegalArgumentException("Unknown transport " + name);
        };
    }
}
