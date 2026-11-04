package io.quarkus.signals.runtime.impl;

import java.util.List;

/**
 * The build-time view of the signals application: the generated receiver classes and the ordered
 * enricher and interceptor identifiers.
 *
 * @param receiversClasses the names of the generated receiver classes
 * @param orderedEnricherIds the enricher identifiers, in execution order
 * @param orderedInterceptorIds the interceptor identifiers, in execution order
 */
public record SignalsContext(List<String> receiversClasses, List<String> orderedEnricherIds,
        List<String> orderedInterceptorIds) {
}
