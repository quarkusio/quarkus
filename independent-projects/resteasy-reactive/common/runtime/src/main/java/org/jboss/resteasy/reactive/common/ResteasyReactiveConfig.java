package org.jboss.resteasy.reactive.common;

public class ResteasyReactiveConfig {

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     */
    private long inputBufferSize;

    /**
     * By default we assume a default produced media type of "text/plain"
     * for String endpoint return types. If this is disabled, the default
     * produced media type will be "[text/plain, *&sol;*]" which is more
     * expensive due to negotiation.
     */
    private boolean singleDefaultProduces;

    public ResteasyReactiveConfig() {
    }

    public ResteasyReactiveConfig(long inputBufferSize, boolean singleDefaultProduces) {
        this.inputBufferSize = inputBufferSize;
        this.singleDefaultProduces = singleDefaultProduces;
    }

    public long getInputBufferSize() {
        return inputBufferSize;
    }

    public boolean isSingleDefaultProduces() {
        return singleDefaultProduces;
    }

    public ResteasyReactiveConfig setInputBufferSize(long inputBufferSize) {
        this.inputBufferSize = inputBufferSize;
        return this;
    }

    public ResteasyReactiveConfig setSingleDefaultProduces(boolean singleDefaultProduces) {
        this.singleDefaultProduces = singleDefaultProduces;
        return this;
    }
}
