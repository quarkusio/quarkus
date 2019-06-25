package io.quarkus.deployment.builditem;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.MethodDescriptor;

/**
 * A build item for execution handlers.
 */
public final class ExecutionHandlerBuildItem extends MultiBuildItem {
    private final MethodDescriptor methodDescriptor;

    /**
     * Construct a new instance.
     *
     * @param methodDescriptor the method descriptor to use to construct the handler (must not be {@code null})
     */
    public ExecutionHandlerBuildItem(final MethodDescriptor methodDescriptor) {
        Assert.checkNotNullParam("methodDescriptor", methodDescriptor);
        this.methodDescriptor = methodDescriptor;
    }

    /**
     * Construct a new instance. The handler will be instantiated by its no-arg constructor.
     *
     * @param handlerClassName the handler class name (must not be {@code null})
     */
    public ExecutionHandlerBuildItem(final String handlerClassName) {
        this(MethodDescriptor.ofConstructor(handlerClassName));
    }

    /**
     * Get the factory method descriptor for this handler.
     *
     * @return the method descriptor
     */
    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }
}
