package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import io.quarkus.runtime.rest.DisabledRestEndpoints;

/**
 * If the RESTEasy Reactive extension passed us a list of REST paths that are disabled via the @DisabledRestEndpoint
 * annotation, remove them from the OpenAPI document. This has to be done at runtime because
 * the annotation is controlled by a runtime config property.
 */
public class DisabledRestEndpointsFilter implements OASFilter {

    public static Optional<OASFilter> maybeGetInstance() {
        var endpoints = DisabledRestEndpoints.get();

        if (endpoints != null && !endpoints.isEmpty()) {
            return Optional.of(new DisabledRestEndpointsFilter(endpoints));
        }

        return Optional.empty();
    }

    final Map<String, List<String>> disabledEndpoints;

    private DisabledRestEndpointsFilter(Map<String, List<String>> disabledEndpoints) {
        this.disabledEndpoints = disabledEndpoints;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();

        disabledEndpoints.entrySet()
                .stream()
                .map(pathMethods -> Map.entry(stripSlash(pathMethods.getKey()), pathMethods.getValue()))
                // Skip paths that are not present in the OpenAPI model
                .filter(pathMethods -> paths.hasPathItem(pathMethods.getKey()))
                .forEach(pathMethods -> {
                    String path = pathMethods.getKey();
                    PathItem pathItem = paths.getPathItem(path);

                    // Remove each operation identified as a disabled HTTP method
                    Optional.ofNullable(pathMethods.getValue())
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .map(PathItem.HttpMethod::valueOf)
                            .forEach(method -> pathItem.setOperation(method, null));

                    if (pathItem.getOperations().isEmpty()) {
                        paths.removePathItem(path);
                    }
                });
    }

    /**
     * Removes any trailing slash character from the path when it is not the root '/'
     * path. This is necessary to align with the paths generated in the OpenAPI model.
     */
    static String stripSlash(String path) {
        if (path.endsWith("/") && path.length() > 1) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
