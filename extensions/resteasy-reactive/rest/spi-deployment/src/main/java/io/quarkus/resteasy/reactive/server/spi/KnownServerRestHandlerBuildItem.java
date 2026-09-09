package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extensions to register the {@link org.jboss.resteasy.reactive.server.spi.ServerRestHandler}
 * classes they add to handler chains (via {@link GlobalHandlerCustomizerBuildItem}, {@link MethodScannerBuildItem}
 * or {@link PreExceptionMapperHandlerBuildItem}).
 * <p>
 * Registered handler classes get a dedicated, monomorphic call site in the generated handler dispatcher,
 * instead of being invoked through a megamorphic interface call.
 * This only makes sense for handlers that run for most requests.
 */
public final class KnownServerRestHandlerBuildItem extends MultiBuildItem {

    private final String className;

    public KnownServerRestHandlerBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
