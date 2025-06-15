package io.quarkus.smallrye.reactivemessaging.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.BroadcastLiteral;
import io.quarkus.smallrye.reactivemessaging.deployment.OnOverflowLiteral;
import io.quarkus.smallrye.reactivemessaging.runtime.EmitterFactoryForLiteral;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusEmitterConfiguration;
import io.smallrye.reactive.messaging.EmitterConfiguration;

/**
 * Represents an emitter injection.
 */
public final class InjectedEmitterBuildItem extends MultiBuildItem {

    /**
     * Creates a new instance of {@link InjectedEmitterBuildItem} setting the overflow strategy.
     *
     * @param name
     *        the name of the stream
     * @param emitterType
     *        emitterType
     * @param overflow
     *        the overflow strategy
     * @param bufferSize
     *        the buffer size, if overflow is set to {@code BUFFER}
     *
     * @return the new {@link InjectedEmitterBuildItem}
     */
    public static InjectedEmitterBuildItem of(String name, String emitterType, String overflow, int bufferSize,
            boolean hasBroadcast, int awaitSubscribers) {
        return new InjectedEmitterBuildItem(name, emitterType, overflow, bufferSize, hasBroadcast, awaitSubscribers);
    }

    /**
     * The name of the stream the emitter is connected to.
     */
    private final String name;

    /**
     * The name of the overflow strategy. Valid values are {@code BUFFER, DROP, FAIL, LATEST, NONE}. If not set, it uses
     * {@code BUFFER} with a default buffer size.
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
     * The emitter type
     */
    private final String emitterType;

    /**
     * If the emitter uses the {@link io.smallrye.reactive.messaging.annotations.Broadcast} annotation, indicates the
     * number of subscribers to be expected before subscribing upstream.
     */
    private final int awaitSubscribers;

    public InjectedEmitterBuildItem(String name, String emitterType, String overflow, int bufferSize,
            boolean hasBroadcast, int awaitSubscribers) {
        this.name = name;
        this.overflow = overflow;
        this.emitterType = emitterType;
        this.bufferSize = bufferSize;
        this.hasBroadcast = hasBroadcast;
        this.awaitSubscribers = hasBroadcast ? awaitSubscribers : -1;
    }

    public EmitterConfiguration getEmitterConfig() {
        return new QuarkusEmitterConfiguration(name, EmitterFactoryForLiteral.of(loadEmitterClass()),
                OnOverflowLiteral.create(overflow, bufferSize),
                hasBroadcast ? new BroadcastLiteral(awaitSubscribers) : null);
    }

    private Class<?> loadEmitterClass() {
        try {
            return Class.forName(emitterType, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

}
