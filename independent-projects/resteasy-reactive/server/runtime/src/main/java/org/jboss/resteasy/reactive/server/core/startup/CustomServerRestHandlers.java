package org.jboss.resteasy.reactive.server.core.startup;

import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public final class CustomServerRestHandlers {

    private final Supplier<ServerRestHandler> blockingInputHandlerSupplier;
    private final Supplier<ServerRestHandler> multipartHandlerSupplier;

    public CustomServerRestHandlers(Supplier<ServerRestHandler> blockingInputHandlerSupplier,
            Supplier<ServerRestHandler> multipartHandlerSupplier) {
        this.blockingInputHandlerSupplier = blockingInputHandlerSupplier;
        this.multipartHandlerSupplier = multipartHandlerSupplier;
    }

    public Supplier<ServerRestHandler> getBlockingInputHandlerSupplier() {
        return blockingInputHandlerSupplier;
    }

    public Supplier<ServerRestHandler> getMultipartHandlerSupplier() {
        return multipartHandlerSupplier;
    }
}
