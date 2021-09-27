package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.jboss.logging.Logger;

import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.security.SecurityRequirementImpl;

/**
 * Automatically add security requirement to RolesAllowed methods
 */
public class AutoRolesAllowedFilter implements OASFilter {
    private static final Logger log = Logger.getLogger(AutoRolesAllowedFilter.class);

    private Map<String, List<String>> methodReferences;
    private String defaultSecuritySchemeName;

    public AutoRolesAllowedFilter() {

    }

    public AutoRolesAllowedFilter(String defaultSecuritySchemeName, Map<String, List<String>> methodReferences) {
        this.defaultSecuritySchemeName = defaultSecuritySchemeName;
        this.methodReferences = methodReferences;
    }

    public Map<String, List<String>> getMethodReferences() {
        return methodReferences;
    }

    public void setMethodReferences(Map<String, List<String>> methodReferences) {
        this.methodReferences = methodReferences;
    }

    public String getDefaultSecuritySchemeName() {
        return defaultSecuritySchemeName;
    }

    public void setDefaultSecuritySchemeName(String defaultSecuritySchemeName) {
        this.defaultSecuritySchemeName = defaultSecuritySchemeName;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        if (!methodReferences.isEmpty()) {
            String securitySchemeName = getSecuritySchemeName(openAPI);
            Paths paths = openAPI.getPaths();
            if (paths != null) {
                Map<String, PathItem> pathItems = paths.getPathItems();
                if (pathItems != null && !pathItems.isEmpty()) {
                    Set<Map.Entry<String, PathItem>> pathItemsEntries = pathItems.entrySet();
                    for (Map.Entry<String, PathItem> pathItem : pathItemsEntries) {
                        Map<PathItem.HttpMethod, Operation> operations = pathItem.getValue().getOperations();
                        if (operations != null && !operations.isEmpty()) {

                            for (Operation operation : operations.values()) {

                                OperationImpl operationImpl = (OperationImpl) operation;

                                if (methodReferences.keySet().contains(operationImpl.getMethodRef())) {
                                    SecurityRequirement securityRequirement = new SecurityRequirementImpl();
                                    List<String> roles = methodReferences.get(operationImpl.getMethodRef());
                                    securityRequirement = securityRequirement.addScheme(securitySchemeName, roles);
                                    operation = operation.addSecurityRequirement(securityRequirement);
                                    APIResponses responses = operation.getResponses();
                                    for (APIResponseImpl response : getSecurityResponses()) {
                                        responses.addAPIResponse(response.getResponseCode(), response);
                                    }
                                    operation = operation.responses(responses);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getSecuritySchemeName(OpenAPI openAPI) {

        // Might be set in annotations
        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null
                && !openAPI.getComponents().getSecuritySchemes().isEmpty()) {
            Map<String, SecurityScheme> securitySchemes = openAPI.getComponents().getSecuritySchemes();
            return securitySchemes.keySet().iterator().next();
        }
        return defaultSecuritySchemeName;
    }

    private List<APIResponseImpl> getSecurityResponses() {
        List<APIResponseImpl> responses = new ArrayList<>();

        APIResponseImpl notAuthorized = new APIResponseImpl();
        notAuthorized.setDescription("Not Authorized");
        notAuthorized.setResponseCode("401");
        responses.add(notAuthorized);

        APIResponseImpl forbidden = new APIResponseImpl();
        forbidden.setDescription("Not Allowed");
        forbidden.setResponseCode("403");
        responses.add(forbidden);

        return responses;
    }

}