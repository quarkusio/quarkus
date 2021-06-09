package org.jboss.resteasy.reactive.server.core.startup;

import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public final class CustomServerRestHandlers {

    private final Supplier<ServerRestHandler> blockingInputHandlerSupplier;

    public CustomServerRestHandlers(Supplier<ServerRestHandler> blockingInputHandlerSupplier) {
        this.blockingInputHandlerSupplier = blockingInputHandlerSupplier;
    }

    public Supplier<ServerRestHandler> getBlockingInputHandlerSupplier() {
        return blockingInputHandlerSupplier;
    }

}
