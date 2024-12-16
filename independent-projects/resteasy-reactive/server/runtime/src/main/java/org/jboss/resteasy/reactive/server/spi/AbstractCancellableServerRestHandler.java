package org.jboss.resteasy.reactive.server.spi;

public abstract class AbstractCancellableServerRestHandler implements ServerRestHandler {

    // make mutable to allow for bytecode serialization
    private boolean cancellable;

    public boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }
}
