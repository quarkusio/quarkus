package io.quarkus.deployment.builditem;

import java.util.Map;
import java.util.logging.Handler;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.common.constraint.Assert;

/**
 * A build item for adding additional named logging handlers.
 */
public final class NamedLogHandlersBuildItem extends MultiBuildItem {
    private final RuntimeValue<Map<String, Handler>> namedHandlersMap;

    /**
     * Construct a new instance.
     *
     * @param namedHandlersMap the named handlers to add to the run time configuration
     */
    public NamedLogHandlersBuildItem(final RuntimeValue<Map<String, Handler>> namedHandlersMap) {
        this.namedHandlersMap = Assert.checkNotNullParam("namedHandlersMap", namedHandlersMap);
    }

    /**
     * Get the named handlers.
     *
     * @return the named handlers map
     */
    public RuntimeValue<Map<String, Handler>> getNamedHandlersMap() {
        return namedHandlersMap;
    }
}
