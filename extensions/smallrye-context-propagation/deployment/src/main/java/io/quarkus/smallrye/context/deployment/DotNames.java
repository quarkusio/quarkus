package io.quarkus.smallrye.context.deployment;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.jandex.DotName;

import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;

public final class DotNames {

    public static final DotName MANAGED_EXECUTOR_CONFIG = DotName.createSimple(ManagedExecutorConfig.class.getName());
    public static final DotName THREAD_CONTEXT_CONFIG = DotName.createSimple(ThreadContextConfig.class.getName());
    public static final DotName NAMED_INSTANCE = DotName.createSimple(NamedInstance.class.getName());
    public static final DotName MANAGED_EXECUTOR = DotName.createSimple(ManagedExecutor.class.getName());
    public static final DotName THREAD_CONTEXT = DotName.createSimple(ThreadContext.class.getName());
}
