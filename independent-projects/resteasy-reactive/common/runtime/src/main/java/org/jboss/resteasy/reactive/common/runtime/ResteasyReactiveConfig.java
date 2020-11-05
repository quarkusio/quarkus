package org.jboss.resteasy.reactive.common.runtime;

public class ResteasyReactiveConfig {

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     */
    public final long inputBufferSize;

    /**
     * By default we assume a default produced media type of "text/plain"
     * for String endpoint return types. If this is disabled, the default
     * produced media type will be "[text/plain, *&sol;*]" which is more
     * expensive due to negotiation.
     */
    public final boolean singleDefaultProduces;

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
}
