package io.quarkus.core;

import java.util.concurrent.Executor;

import io.quarkus.core.impl.ServiceNode;
import io.smallrye.common.constraint.Assert;

/**
 * Set the executor to be used for service startup and shutdown.
 * <p>
 * <b>WARNING:</b> This class is for internal Quarkus use only.
 * It must only be called from the thread pool setup service.
 * Calling it from extension code will cause unpredictable behavior.
 */
public final class ServiceExecutorAccess {

    private ServiceExecutorAccess() {
    }

    /**
     * Get and set the executor to use for future service graph submissions.
     * <p>
     * <b>WARNING:</b> This method is for internal Quarkus use only.
     * It must only be called from the thread pool setup service.
     * Calling it from extension code will cause unpredictable behavior.
     *
     * @param context the start context (must not be {@code null})
     * @param executor the new executor (must not be {@code null})
     * @return the previous executor (not {@code null})
     */
    public static Executor setExecutor(final StartContext context, final Executor executor) {
        Assert.checkNotNullParam("context", context);
        Assert.checkNotNullParam("executor", executor);
        return ((ServiceNode) context).graph().setExecutor(executor);
    }
}
