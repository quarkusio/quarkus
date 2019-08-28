package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class EmitterBuildItem extends MultiBuildItem {

    /**
     * The name of the stream the emitter is connected to.
     */
    private final String name;

    private String overflow;

    private int bufferSize;

    public EmitterBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getOverflow() {
        return overflow;
    }

    public EmitterBuildItem setOverflow(String overflow) {
        this.overflow = overflow;
        return this;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public EmitterBuildItem setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }
}
