package io.quarkus.smallrye.reactivemessaging.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.BroadcastLiteral;
import io.quarkus.smallrye.reactivemessaging.deployment.OnOverflowLiteral;
import io.smallrye.reactive.messaging.providers.extension.EmitterConfiguration;

/**
 * Represents an emitter injection.
 */
public final class InjectedEmitterBuildItem extends MultiBuildItem {

    /**
     * Creates a new instance of {@link InjectedEmitterBuildItem} setting the overflow strategy.
     *
     * @param name the name of the stream
     * @param isMutinyEmitter if the emitter is a {@link io.smallrye.reactive.messaging.MutinyEmitter}
     * @param overflow the overflow strategy
     * @param bufferSize the buffer size, if overflow is set to {@code BUFFER}
     * @return the new {@link InjectedEmitterBuildItem}
     */
    public static InjectedEmitterBuildItem of(String name, boolean isMutinyEmitter, String overflow, int bufferSize,
            boolean hasBroadcast,
            int awaitSubscribers) {
        return new InjectedEmitterBuildItem(name, isMutinyEmitter, overflow, bufferSize, hasBroadcast, awaitSubscribers);
    }

    /**
     * The name of the stream the emitter is connected to.
     */
    private final String name;

    /**
     * The name of the overflow strategy. Valid values are {@code BUFFER, DROP, FAIL, LATEST, NONE}.
     * If not set, it uses {@code BUFFER} with a default buffer size.
     */
    private final String overflow;

    /**
     * The buffer size, used when {@code overflow} is set to {@code BUFFER}. Not that if {@code overflow} is set to
     * {@code BUFFER} and {@code bufferSize} is not set, an unbounded buffer is used.
     */
    private final int bufferSize;

    /**
     * Whether the emitter uses the {@link io.smallrye.reactive.messaging.annotations.Broadcast} annotation.
     */
    private final boolean hasBroadcast;

    /**
     * Whether the emitter is a {@link io.smallrye.reactive.messaging.MutinyEmitter} or a regular (non-mutiny) emitter.
     */
    private final boolean isMutinyEmitter;

    /**
     * If the emitter uses the {@link io.smallrye.reactive.messaging.annotations.Broadcast} annotation, indicates the
     * number of subscribers to be expected before subscribing upstream.
     */
    private final int awaitSubscribers;

    public InjectedEmitterBuildItem(String name, boolean isMutinyEmitter, String overflow, int bufferSize, boolean hasBroadcast,
            int awaitSubscribers) {
        this.name = name;
        this.overflow = overflow;
        this.isMutinyEmitter = isMutinyEmitter;
        this.bufferSize = bufferSize;
        this.hasBroadcast = hasBroadcast;
        this.awaitSubscribers = hasBroadcast ? awaitSubscribers : -1;
    }

    public EmitterConfiguration getEmitterConfig() {
        return new EmitterConfiguration(name, isMutinyEmitter, OnOverflowLiteral.create(overflow, bufferSize),
                hasBroadcast ? new BroadcastLiteral(awaitSubscribers) : null);
    }

}
