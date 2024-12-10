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

/**
 * This filter replaces the former AutoTagFilter and AutoRolesAllowedFilter and has three functions:
 * <ul>
 * <li>Add operation descriptions based on the associated Java method name handling the operation
 * <li>Add operation tags based on the associated Java class of the operation
 * <li>Add security requirements based on discovered {@link jakarta.annotation.security.RolesAllowed},
 * {@link io.quarkus.security.PermissionsAllowed}, and {@link io.quarkus.security.Authenticated}
 * annotations.
 * </ul>
 */
public class OperationFilter implements OASFilter {

    public static final String EXT_METHOD_REF = "x-quarkus-openapi-method-ref";

    private final Map<String, ClassAndMethod> classNameMap;
    private final Map<String, List<String>> rolesAllowedMethodReferences;
    private final List<String> authenticatedMethodReferences;
    private final String defaultSecuritySchemeName;
    private final boolean doAutoTag;
    private final boolean doAutoOperation;
    private final boolean alwaysIncludeScopesValidForScheme;

    public OperationFilter(Map<String, ClassAndMethod> classNameMap,
            Map<String, List<String>> rolesAllowedMethodReferences,
            List<String> authenticatedMethodReferences,
            String defaultSecuritySchemeName,
            boolean doAutoTag, boolean doAutoOperation, boolean alwaysIncludeScopesValidForScheme) {

        this.classNameMap = Objects.requireNonNull(classNameMap);
        this.rolesAllowedMethodReferences = Objects.requireNonNull(rolesAllowedMethodReferences);
        this.authenticatedMethodReferences = Objects.requireNonNull(authenticatedMethodReferences);
        this.defaultSecuritySchemeName = Objects.requireNonNull(defaultSecuritySchemeName);
        this.doAutoTag = doAutoTag;
        this.doAutoOperation = doAutoOperation;
        this.alwaysIncludeScopesValidForScheme = alwaysIncludeScopesValidForScheme;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        var securityScheme = getSecurityScheme(openAPI);
        String schemeName = securityScheme.map(Map.Entry::getKey).orElse(defaultSecuritySchemeName);
        boolean scopesValidForScheme = alwaysIncludeScopesValidForScheme || securityScheme.map(Map.Entry::getValue)
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
                    final String methodRef = methodRef(operation);

                    if (methodRef != null) {
                        maybeSetSummaryAndTag(operation, methodRef);
                        maybeAddSecurityRequirement(operation, methodRef, schemeName, scopesValidForScheme,
                                defaultSecurityErrors);
                    }

                    operation.removeExtension(EXT_METHOD_REF);
                });
    }

    private String methodRef(Operation operation) {
        final Map<String, Object> extensions = operation.getExtensions();
        return (String) (extensions != null ? extensions.get(EXT_METHOD_REF) : null);
    }

    private void maybeSetSummaryAndTag(Operation operation, String methodRef) {
        if (!classNameMap.containsKey(methodRef)) {
            return;
        }

        ClassAndMethod classMethod = classNameMap.get(methodRef);

        if (doAutoOperation && operation.getSummary() == null) {
            // Auto add a summary
            operation.setSummary(capitalizeFirstLetter(splitCamelCase(classMethod.method().name())));
        }

        if (doAutoTag && (operation.getTags() == null || operation.getTags().isEmpty())) {
            operation.addTag(splitCamelCase(classMethod.classInfo().simpleName()));
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

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void maybeAddSecurityRequirement(Operation operation, String methodRef, String schemeName, boolean allowScopes,
            Map<String, APIResponse> defaultSecurityErrors) {
        if (rolesAllowedMethodReferences.containsKey(methodRef)) {
            List<String> scopes = rolesAllowedMethodReferences.get(methodRef);
            addSecurityRequirement(operation, schemeName, allowScopes ? scopes : Collections.emptyList());
            addDefaultSecurityResponses(operation, defaultSecurityErrors);
        } else if (authenticatedMethodReferences.contains(methodRef)) {
            addSecurityRequirement(operation, schemeName, Collections.emptyList());
            addDefaultSecurityResponses(operation, defaultSecurityErrors);
        }
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
