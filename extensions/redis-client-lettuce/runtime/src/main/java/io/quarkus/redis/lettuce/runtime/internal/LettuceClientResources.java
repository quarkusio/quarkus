package io.quarkus.redis.lettuce.runtime.internal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.channel.EventLoopGroup;

/**
 * Provides Lettuce {@link ClientResources} configured to reuse Vert.x-managed Netty event loops
 * via a {@link VertxEventLoopGroupProvider}.
 * <p>
 * Lettuce will not create its own event loop threads. Timer, computation executor, and DNS resolver
 * remain Lettuce-owned and are shut down when {@link #shutdown()} is called.
 * <p>
 * A client whose configuration sets the reconnect delay or Netty channel options, which Lettuce only takes from
 * its {@link ClientResources}, gets its own instance built on the shared threads (see {@link #clientResources}).
 */
public class LettuceClientResources {

    private static final Logger LOGGER = Logger.getLogger(LettuceClientResources.class);

    private final ClientResources clientResources;
    private final List<ClientResources> derived = new CopyOnWriteArrayList<>();

    /**
     * Creates Lettuce {@link ClientResources} that share the given Vert.x-managed event loop group.
     *
     * @param vertxEventLoopGroup the Vert.x-managed Netty event loop group
     */
    public LettuceClientResources(EventLoopGroup vertxEventLoopGroup) {
        LOGGER.info("Creating Lettuce ClientResources with shared Vert.x event loops");

        VertxEventLoopGroupProvider eventLoopGroupProvider = new VertxEventLoopGroupProvider(vertxEventLoopGroup);

        this.clientResources = ClientResources.builder()
                .eventLoopGroupProvider(eventLoopGroupProvider)
                // LatencyUtils and HdrHistogram are on the classpath (needed for native images), which would make
                // Lettuce enable its command latency collector by default: every command gets wrapped and recorded,
                // a pause detector thread is started and latency events are published with no consumer.
                .commandLatencyRecorder(CommandLatencyRecorder.disabled())
                .build();
    }

    /**
     * Returns the configured {@link ClientResources} instance.
     */
    public ClientResources clientResources() {
        return clientResources;
    }

    /**
     * The {@link ClientResources} of a client: the shared instance, unless the client configures a reconnect delay
     * or Netty channel options (see {@link LettuceClientSettings#reconnectDelay()} and
     * {@link LettuceClientSettings#nettyCustomizer()}), which Lettuce only takes from the resources. Such a client gets
     * its own instance carrying them, built on the event loops, computation threads, timer, event bus and address
     * resolvers of the shared one so that no thread is duplicated: Lettuce leaves components it was given running when
     * the instance is shut down, so {@link #shutdown()} shuts the derived instances down before the shared one. (The
     * instance is not derived with {@code ClientResources.mutate()}, which copies the ownership of the timer and the
     * computation threads from the shared instance and would stop them with the derived one.)
     *
     * @param clientName the Quarkus Redis client name, used for logging
     * @param reconnectDelay the delay between reconnection attempts, empty for the Lettuce default
     * @param nettyCustomizer the channel options to set on the connections, empty for none
     */
    public ClientResources clientResources(String clientName, Optional<Delay> reconnectDelay,
            Optional<NettyCustomizer> nettyCustomizer) {
        if (reconnectDelay.isEmpty() && nettyCustomizer.isEmpty()) {
            return clientResources;
        }
        LOGGER.infof("Creating Lettuce ClientResources for client '%s' with %s%s%s, on the shared Vert.x event loops",
                clientName, reconnectDelay.isPresent() ? "its reconnect delay" : "",
                reconnectDelay.isPresent() && nettyCustomizer.isPresent() ? " and " : "",
                nettyCustomizer.isPresent() ? "its channel options" : "");
        ClientResources.Builder builder = ClientResources.builder()
                .eventLoopGroupProvider(clientResources.eventLoopGroupProvider())
                .eventExecutorGroup(clientResources.eventExecutorGroup())
                .timer(clientResources.timer())
                .eventBus(clientResources.eventBus())
                .addressResolverGroup(clientResources.addressResolverGroup())
                .socketAddressResolver(clientResources.socketAddressResolver())
                .commandLatencyRecorder(CommandLatencyRecorder.disabled());
        if (reconnectDelay.isPresent()) {
            builder.reconnectDelay(reconnectDelay.get());
        }
        if (nettyCustomizer.isPresent()) {
            builder.nettyCustomizer(nettyCustomizer.get());
        }
        ClientResources resources = builder.build();
        derived.add(resources);
        return resources;
    }

    /**
     * Shuts down Lettuce-owned resources (timer, computation executor, DNS resolver).
     * Does <b>not</b> shut down the Vert.x-managed event loop group.
     *
     * @param quietPeriodMs quiet period in milliseconds before forceful shutdown
     * @param timeoutMs maximum time to wait for shutdown in milliseconds
     */
    public void shutdown(long quietPeriodMs, long timeoutMs) {
        LOGGER.info("Shutting down Lettuce ClientResources (event loops remain Vert.x-managed)");
        // the derived instances own nothing but their settings: this only marks them as shut down
        for (ClientResources resources : derived) {
            resources.shutdown(quietPeriodMs, timeoutMs, TimeUnit.MILLISECONDS);
        }
        derived.clear();
        clientResources.shutdown(quietPeriodMs, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down with default quiet period (100ms) and timeout (2000ms).
     */
    public void shutdown() {
        shutdown(100, 2000);
    }
}
