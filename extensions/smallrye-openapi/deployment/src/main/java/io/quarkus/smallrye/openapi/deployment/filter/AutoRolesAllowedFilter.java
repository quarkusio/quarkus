package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import io.smallrye.openapi.api.models.OperationImpl;

/**
 * Automatically add security requirement to RolesAllowed methods
 */
public class AutoRolesAllowedFilter implements OASFilter {
    private Map<String, List<String>> rolesAllowedMethodReferences;
    private List<String> authenticatedMethodReferences;
    private String defaultSecuritySchemeName;

    public AutoRolesAllowedFilter() {

    }

    public AutoRolesAllowedFilter(String defaultSecuritySchemeName, Map<String, List<String>> rolesAllowedMethodReferences,
            List<String> authenticatedMethodReferences) {
        this.defaultSecuritySchemeName = defaultSecuritySchemeName;
        this.rolesAllowedMethodReferences = rolesAllowedMethodReferences;
        this.authenticatedMethodReferences = authenticatedMethodReferences;
    }

    public Map<String, List<String>> getRolesAllowedMethodReferences() {
        return rolesAllowedMethodReferences;
    }

    public void setRolesAllowedMethodReferences(Map<String, List<String>> rolesAllowedMethodReferences) {
        this.rolesAllowedMethodReferences = rolesAllowedMethodReferences;
    }

    public boolean hasRolesAllowedMethodReferences() {
        return this.rolesAllowedMethodReferences != null && !this.rolesAllowedMethodReferences.isEmpty();
    }

    public boolean hasRolesAllowedMethodReference(String methodRef) {
        return this.rolesAllowedMethodReferences != null && this.rolesAllowedMethodReferences.containsKey(methodRef);
    }

    public List<String> getAuthenticatedMethodReferences() {
        return authenticatedMethodReferences;
    }

    public void setAuthenticatedMethodReferences(List<String> authenticatedMethodReferences) {
        this.authenticatedMethodReferences = authenticatedMethodReferences;
    }

    public boolean hasAuthenticatedMethodReferences() {
        return this.authenticatedMethodReferences != null && !this.authenticatedMethodReferences.isEmpty();
    }

    public boolean hasAuthenticatedMethodReference(String methodRef) {
        return this.authenticatedMethodReferences != null && this.authenticatedMethodReferences.contains(methodRef);
    }

    public String getDefaultSecuritySchemeName() {
        return defaultSecuritySchemeName;
    }

    public void setDefaultSecuritySchemeName(String defaultSecuritySchemeName) {
        this.defaultSecuritySchemeName = defaultSecuritySchemeName;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (!hasRolesAllowedMethodReferences() && !hasAuthenticatedMethodReferences()) {
            return;
        }

        var securityScheme = getSecurityScheme(openAPI);
        String schemeName = securityScheme.map(Map.Entry::getKey).orElse(defaultSecuritySchemeName);
        boolean scopesValidForScheme = securityScheme.map(Map.Entry::getValue)
                .map(SecurityScheme::getType)
                .map(Set.of(SecurityScheme.Type.OAUTH2, SecurityScheme.Type.OPENIDCONNECT)::contains)
                .orElse(false);
        Map<String, APIResponse> defaultSecurityErrors = getSecurityResponses();

        Optional.ofNullable(openAPI.getPaths())
                .map(Paths::getPathItems)
                .map(Map::entrySet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(Map.Entry::getValue)
                .map(PathItem::getOperations)
                .filter(Objects::nonNull)
                .map(Map::values)
                .flatMap(Collection::stream)
                .forEach(operation -> {
                    String methodRef = OperationImpl.getMethodRef(operation);

                    if (hasRolesAllowedMethodReference(methodRef)) {
                        List<String> scopes = rolesAllowedMethodReferences.get(methodRef);
                        addSecurityRequirement(operation, schemeName, scopesValidForScheme ? scopes : Collections.emptyList());
                        addDefaultSecurityResponses(operation, defaultSecurityErrors);
                    } else if (hasAuthenticatedMethodReference(methodRef)) {
                        addSecurityRequirement(operation, schemeName, Collections.emptyList());
                        addDefaultSecurityResponses(operation, defaultSecurityErrors);
                    }
                });
    }

    private Optional<Map.Entry<String, SecurityScheme>> getSecurityScheme(OpenAPI openAPI) {
        // Might be set in annotations
        return Optional.ofNullable(openAPI.getComponents())
                .map(Components::getSecuritySchemes)
                .map(Map::entrySet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .findFirst();
    }

    private void addSecurityRequirement(Operation operation, String schemeName, List<String> scopes) {
        SecurityRequirement securityRequirement = OASFactory.createSecurityRequirement();
        securityRequirement = securityRequirement.addScheme(schemeName, scopes);
        operation.addSecurityRequirement(securityRequirement);
    }

    private void addDefaultSecurityResponses(Operation operation, Map<String, APIResponse> defaultSecurityErrors) {
        APIResponses responses = operation.getResponses();

        defaultSecurityErrors.entrySet()
                .stream()
                .filter(e -> !responses.hasAPIResponse(e.getKey()))
                .forEach(e -> responses.addAPIResponse(e.getKey(), e.getValue()));
    }

    private Map<String, APIResponse> getSecurityResponses() {
        Map<String, APIResponse> responses = new LinkedHashMap<>();
        responses.put("401", OASFactory.createAPIResponse().description("Not Authorized"));
        responses.put("403", OASFactory.createAPIResponse().description("Not Allowed"));
        return responses;
    }

}