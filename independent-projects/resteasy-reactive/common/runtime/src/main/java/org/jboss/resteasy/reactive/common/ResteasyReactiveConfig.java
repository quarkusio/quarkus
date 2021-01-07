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

    /**
     * When one of the quarkus-resteasy-reactive-jackson or quarkus-resteasy-reactive-jsonb extension are active
     * and the result type of an endpoint is an application class or one of {@code Collection}, {@code List}, {@code Set} or
     * {@code Map}, we assume the default return type is "application/json".
     */
    private boolean defaultProduces;

    // we need this (and the setters) due to Bytecode Recording
    public ResteasyReactiveConfig() {
    }

    public ResteasyReactiveConfig(long inputBufferSize, boolean singleDefaultProduces, boolean defaultProduces) {
        this.inputBufferSize = inputBufferSize;
        this.singleDefaultProduces = singleDefaultProduces;
        this.defaultProduces = defaultProduces;
    }

    public long getInputBufferSize() {
        return inputBufferSize;
    }

    public void setInputBufferSize(long inputBufferSize) {
        this.inputBufferSize = inputBufferSize;
    }

    public boolean isSingleDefaultProduces() {
        return singleDefaultProduces;
    }

    public void setSingleDefaultProduces(boolean singleDefaultProduces) {
        this.singleDefaultProduces = singleDefaultProduces;
    }

    public boolean isDefaultProduces() {
        return defaultProduces;
    }

    public void setDefaultProduces(boolean defaultProduces) {
        this.defaultProduces = defaultProduces;
    }
}
