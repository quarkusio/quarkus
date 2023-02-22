package io.quarkus.deployment.builditem;

import java.util.Optional;
import java.util.logging.Handler;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * A build item for adding the dev stream log via mutiny
 */
public final class StreamingLogHandlerBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Optional<Handler>> handlerValue;

    /**
     * Construct a new instance.
     *
     * @param handlerValue the handler value to add to the run time configuration
     */
    public StreamingLogHandlerBuildItem(final RuntimeValue<Optional<Handler>> handlerValue) {
        this.handlerValue = Assert.checkNotNullParam("handlerValue", handlerValue);
    }

    /**
     * Get the handler value.
     *
     * @return the handler value
     */
    public RuntimeValue<Optional<Handler>> getHandlerValue() {
        return handlerValue;
    }
}
