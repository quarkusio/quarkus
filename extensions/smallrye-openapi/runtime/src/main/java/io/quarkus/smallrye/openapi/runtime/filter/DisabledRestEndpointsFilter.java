package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();

        disabledRestEndpoints()
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

    static Stream<Map.Entry<String, List<String>>> disabledRestEndpoints() {
        return Optional.ofNullable(DisabledRestEndpoints.get())
                .orElseGet(Collections::emptyMap)
                .entrySet()
                .stream()
                .map(pathMethods -> Map.entry(stripSlash(pathMethods.getKey()), pathMethods.getValue()));
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
