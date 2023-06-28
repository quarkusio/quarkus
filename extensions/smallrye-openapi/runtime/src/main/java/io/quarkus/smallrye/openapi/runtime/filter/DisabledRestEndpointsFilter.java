package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;

import io.quarkus.runtime.rest.DisabledRestEndpoints;

/**
 * If the RESTEasy Reactive extension passed us a list of REST paths that are disabled via the @DisabledRestEndpoint
 * annotation, remove them from the OpenAPI document. This has to be done at runtime because
 * the annotation is controlled by a runtime config property.
 */
public class DisabledRestEndpointsFilter implements OASFilter {

    public void filterOpenAPI(OpenAPI openAPI) {
        Map<String, List<String>> disabledEndpointsMap = DisabledRestEndpoints.get();
        if (disabledEndpointsMap != null) {
            Map<String, PathItem> pathItems = openAPI.getPaths().getPathItems();
            List<String> emptyPathItems = new ArrayList<>();
            if (pathItems != null) {
                for (Map.Entry<String, PathItem> entry : pathItems.entrySet()) {
                    String path = entry.getKey();
                    PathItem pathItem = entry.getValue();
                    List<String> disabledMethodsForThisPath = disabledEndpointsMap.get(path);
                    if (disabledMethodsForThisPath != null) {
                        disabledMethodsForThisPath.forEach(method -> {
                            pathItem.setOperation(PathItem.HttpMethod.valueOf(method), null);
                        });
                        // if the pathItem is now empty, remove it
                        if (pathItem.getOperations().isEmpty()) {
                            emptyPathItems.add(path);
                        }
                    }
                }
                emptyPathItems.forEach(openAPI.getPaths()::removePathItem);
            }
        }
    }
}
