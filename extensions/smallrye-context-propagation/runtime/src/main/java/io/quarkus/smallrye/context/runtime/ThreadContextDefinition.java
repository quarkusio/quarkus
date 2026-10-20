package io.quarkus.smallrye.context.runtime;

import java.util.List;

/**
 * The settings of one configured {@link org.eclipse.microprofile.context.ThreadContext} bean, captured at build time
 * from a {@code @ThreadContextConfig} annotation or from its defaults.
 *
 * @param cleared the context types to clear
 * @param propagated the context types to propagate
 * @param unchanged the context types to leave unchanged
 */
public record ThreadContextDefinition(List<String> cleared, List<String> propagated, List<String> unchanged) {
}
