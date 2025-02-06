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
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * This filter has the following functions:
 * <ul>
 * <li>Add operation descriptions based on the associated Java method name handling the operation</li>
 * <li>Add operation tags based on the associated Java class of the operation</li>
 * <li>Add security requirements based on discovered {@link jakarta.annotation.security.RolesAllowed},
 * {@link io.quarkus.security.PermissionsAllowed}, and {@link io.quarkus.security.Authenticated}
 * annotations. Also add the expected security responses if needed.</li>
 * <li>Add Bad Request (400) response for invalid input (if none is provided)</li>
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
    private final boolean doAutoBadRequest;
    private final boolean alwaysIncludeScopesValidForScheme;

    public OperationFilter(Map<String, ClassAndMethod> classNameMap,
            Map<String, List<String>> rolesAllowedMethodReferences,
            List<String> authenticatedMethodReferences,
            String defaultSecuritySchemeName,
            boolean doAutoTag, boolean doAutoOperation, boolean doAutoBadRequest, boolean alwaysIncludeScopesValidForScheme) {

        this.classNameMap = Objects.requireNonNull(classNameMap);
        this.rolesAllowedMethodReferences = Objects.requireNonNull(rolesAllowedMethodReferences);
        this.authenticatedMethodReferences = Objects.requireNonNull(authenticatedMethodReferences);
        this.defaultSecuritySchemeName = Objects.requireNonNull(defaultSecuritySchemeName);
        this.doAutoTag = doAutoTag;
        this.doAutoOperation = doAutoOperation;
        this.doAutoBadRequest = doAutoBadRequest;
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
                .flatMap(operations -> operations.entrySet().stream())
                .forEach(operation -> {
                    final String methodRef = methodRef(operation.getValue());

                    if (methodRef != null) {
                        maybeSetSummaryAndTag(operation.getValue(), methodRef);
                        maybeAddSecurityRequirement(operation.getValue(), methodRef, schemeName, scopesValidForScheme,
                                defaultSecurityErrors);
                        maybeAddBadRequestResponse(openAPI, operation, methodRef);
                    }

                    operation.getValue().removeExtension(EXT_METHOD_REF);
                });
    }

    private String methodRef(Operation operation) {
        final Map<String, Object> extensions = operation.getExtensions();
        return (String) (extensions != null ? extensions.get(EXT_METHOD_REF) : null);
    }

    private void maybeAddBadRequestResponse(OpenAPI openAPI, Map.Entry<PathItem.HttpMethod, Operation> operation,
            String methodRef) {
        if (!classNameMap.containsKey(methodRef)) {
            return;
        }

        if (doAutoBadRequest
                && isPOSTorPUT(operation) // Only applies to PUT and POST
                && hasBody(operation) // Only applies to input
                && !isStringOrNumberOrBoolean(operation, openAPI) // Except String, Number and boolean
                && !isFileUpload(operation, openAPI)) { // and file
            if (!operation.getValue().getResponses().hasAPIResponse("400")) { // Only when the user has not already added one
                operation.getValue().getResponses().addAPIResponse("400",
                        OASFactory.createAPIResponse().description("Bad Request"));
            }
        }
    }

    private boolean isPOSTorPUT(Map.Entry<PathItem.HttpMethod, Operation> operation) {
        return operation.getKey().equals(PathItem.HttpMethod.POST)
                || operation.getKey().equals(PathItem.HttpMethod.PUT);
    }

    private boolean hasBody(Map.Entry<PathItem.HttpMethod, Operation> operation) {
        return operation.getValue().getRequestBody() != null;
    }

    private boolean isStringOrNumberOrBoolean(Map.Entry<PathItem.HttpMethod, Operation> operation, OpenAPI openAPI) {
        boolean isStringOrNumberOrBoolean = false;
        Content content = operation.getValue().getRequestBody().getContent();
        if (content != null) {
            for (MediaType mediaType : content.getMediaTypes().values()) {
                if (mediaType != null && mediaType.getSchema() != null) {
                    Schema schema = mediaType.getSchema();

                    if (schema.getRef() != null
                            || (schema.getContentSchema() != null && schema.getContentSchema().getRef() != null))
                        schema = resolveSchema(schema, openAPI.getComponents());
                    if (isString(schema) || isNumber(schema) || isBoolean(schema)) {
                        isStringOrNumberOrBoolean = true;
                    }
                }
            }
        }
        return isStringOrNumberOrBoolean;
    }

    private Schema resolveSchema(Schema schema, Components components) {
        while (schema != null) {
            // Resolve `$ref` schema
            if (schema.getRef() != null && components != null) {
                String refName = schema.getRef().replace("#/components/schemas/", "");
                schema = components.getSchemas().get(refName);
                if (schema == null)
                    break;
            } else if (schema.getContentSchema() != null) {
                schema = schema.getContentSchema();
                continue;
            }

            break;
        }
        return schema;
    }

    private boolean isFileUpload(Map.Entry<PathItem.HttpMethod, Operation> operation, OpenAPI openAPI) {
        boolean isFile = false;
        Content content = operation.getValue().getRequestBody().getContent();
        if (content != null) {
            for (Map.Entry<String, MediaType> kv : content.getMediaTypes().entrySet()) {
                String mediaTypeKey = kv.getKey();
                if ("multipart/form-data".equals(mediaTypeKey) || "application/octet-stream".equals(mediaTypeKey)) {
                    MediaType mediaType = kv.getValue();
                    if (mediaType != null && mediaType.getSchema() != null) {
                        if (isFileSchema(mediaType.getSchema(), openAPI.getComponents())) {
                            isFile = true;
                        }
                    }
                }
            }
        }
        return isFile;
    }

    private boolean isFileSchema(Schema schema, Components components) {
        if (isString(schema) && isBinaryFormat(schema)) {
            return true; // Direct file schema
        }
        if (isObject(schema) && schema.getProperties() != null) {
            // Check if it has a "file" property with type "string" and format "binary"
            return schema.getProperties().values().stream()
                    .anyMatch(prop -> isString(prop) && isBinaryFormat(prop));
        }
        if (schema.getRef() != null && components != null) {
            // Resolve reference and check recursively
            String refName = schema.getRef().replace("#/components/schemas/", "");
            Schema referencedSchema = components.getSchemas().get(refName);
            if (referencedSchema != null) {
                return isFileSchema(referencedSchema, components);
            }
        }
        return false;
    }

    private boolean isString(Schema schema) {
        return schema != null && schema.getType() != null && schema.getType().contains(Schema.SchemaType.STRING);
    }

    private boolean isNumber(Schema schema) {
        return schema != null && schema.getType() != null && (schema.getType().contains(Schema.SchemaType.INTEGER)
                || schema.getType().contains(Schema.SchemaType.NUMBER));
    }

    private boolean isBoolean(Schema schema) {
        return schema != null && schema.getType() != null && schema.getType().contains(Schema.SchemaType.BOOLEAN);
    }

    private boolean isObject(Schema schema) {
        return schema != null && schema.getType() != null && schema.getType().contains(Schema.SchemaType.OBJECT);
    }

    private boolean isBinaryFormat(Schema schema) {
        return "binary".equals(schema.getFormat());
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
