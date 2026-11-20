package io.quarkus.redis.lettuce.runtime.internal;

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
 */
public final class LettuceTransport {

    private static final Logger LOGGER = Logger.getLogger(LettuceTransport.class);

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
        System.setProperty("io.lettuce.core.epoll", Boolean.toString("epoll".equals(name)));
        System.setProperty("io.lettuce.core.kqueue", Boolean.toString("kqueue".equals(name)));
        System.setProperty("io.lettuce.core.iouring", Boolean.toString("io_uring".equals(name)));
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
