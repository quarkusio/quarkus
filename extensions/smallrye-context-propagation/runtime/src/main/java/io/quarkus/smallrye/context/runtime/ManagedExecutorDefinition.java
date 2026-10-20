package io.quarkus.smallrye.context.runtime;

import java.util.List;

/**
 * The settings of one configured {@link org.eclipse.microprofile.context.ManagedExecutor} bean, captured at build time
 * from a {@code @ManagedExecutorConfig} annotation or from its defaults.
 *
 * @param cleared the context types to clear
 * @param propagated the context types to propagate
 * @param maxAsync the maximum number of concurrent asynchronous tasks, or {@code -1} for no limit
 * @param maxQueued the maximum number of queued tasks, or {@code -1} for no limit
 */
public record ManagedExecutorDefinition(List<String> cleared, List<String> propagated, int maxAsync, int maxQueued) {
}
