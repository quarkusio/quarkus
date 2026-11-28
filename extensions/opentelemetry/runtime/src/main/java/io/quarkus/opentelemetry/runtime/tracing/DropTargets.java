package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

/**
 * The paths of the non-application endpoints and of the static resources, which are not sampled unless configured
 * otherwise.
 *
 * @param nonApplicationUris the paths of the non-application endpoints
 * @param staticResources the paths of the static resources
 */
public record DropTargets(List<String> nonApplicationUris, List<String> staticResources) {
}
