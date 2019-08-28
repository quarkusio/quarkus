package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class EmitterBuildItem extends MultiBuildItem {

    /**
     * Creates a new instance of {@link EmitterBuildItem} setting the overflow strategy.
     *
     * @param name the name of the stream
     * @param overflow the overflow strategy
     * @param bufferSize the buffer size, if overflow is set to {@code BUFFER}
     * @return the new {@link EmitterBuildItem}
     */
    static EmitterBuildItem of(String name, String overflow, int bufferSize) {
        return new EmitterBuildItem(name, overflow, bufferSize);
    }

    /**
     * Creates a new instance of {@link EmitterBuildItem} using the default overflow strategy.
     *
     * @param name the name of the stream
     * @return the new {@link EmitterBuildItem}
     */
    static EmitterBuildItem of(String name) {
        return new EmitterBuildItem(name, null, -1);
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

    public EmitterBuildItem(String name, String overflow, int bufferSize) {
        this.name = name;
        this.overflow = overflow;
        this.bufferSize = bufferSize;
    }

    public String getName() {
        return name;
    }

    public String getOverflow() {
        return overflow;
    }

    public int getBufferSize() {
        return bufferSize;
    }

}
