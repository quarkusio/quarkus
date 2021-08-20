package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.jboss.logging.Logger;

import io.smallrye.openapi.api.models.OperationImpl;

/**
 * Automatically tag operations based on the class name.
 */
public class AutoTagFilter implements OASFilter {
    private static final Logger log = Logger.getLogger(AutoTagFilter.class);

    private Map<String, String> classNameMap;

    public AutoTagFilter() {

    }

    public AutoTagFilter(Map<String, String> classNameMap) {
        this.classNameMap = classNameMap;
    }

    public Map<String, String> getClassNameMap() {
        return classNameMap;
    }

    public void setClassNameMap(Map<String, String> classNameMap) {
        this.classNameMap = classNameMap;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (!classNameMap.isEmpty()) {
            Paths paths = openAPI.getPaths();
            if (paths != null) {
                Map<String, PathItem> pathItems = paths.getPathItems();
                if (pathItems != null && !pathItems.isEmpty()) {
                    Set<Map.Entry<String, PathItem>> pathItemsEntries = pathItems.entrySet();
                    for (Map.Entry<String, PathItem> pathItem : pathItemsEntries) {
                        Map<PathItem.HttpMethod, Operation> operations = pathItem.getValue().getOperations();
                        if (operations != null && !operations.isEmpty()) {
                            for (Operation operation : operations.values()) {
                                if (operation.getTags() == null || operation.getTags().isEmpty()) {
                                    // Auto add a tag
                                    OperationImpl operationImpl = (OperationImpl) operation;
                                    String methodRef = operationImpl.getMethodRef();
                                    if (classNameMap.containsKey(methodRef)) {
                                        operation.addTag(splitCamelCase(classNameMap.get(methodRef)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"),
                " ");
    }
}