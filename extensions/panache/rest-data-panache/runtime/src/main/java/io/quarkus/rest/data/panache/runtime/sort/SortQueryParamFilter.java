package io.quarkus.rest.data.panache.runtime.sort;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.rest.data.panache.runtime.RestDataPanacheConfig;
import io.quarkus.security.identity.SecurityIdentity;

@Provider
@SortQueryParamValidator
public class SortQueryParamFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(SortQueryParamFilter.class);

    // Unicode range -￾ allows extended characters in identifiers (same as Java identifier rules).
    // The original regex used character ranges [a-z], [A-Z], [0-9] which is equivalent to the explicit
    // alternation used here, but we spell out the components for clarity.
    // Nested properties (e.g., author.name) are not supported - only direct entity fields.
    private static final String IDENTIFIER = "([a-z]|[A-Z]|_|\\$|[\\u0080-\\ufffe])([a-z]|[A-Z]|_|\\$|[0-9]|[\\u0080-\\ufffe])*";
    private static final String SORT_REGEX = "-?" + IDENTIFIER;

    /**
     * Reserved query parameter names used by the REST Data Panache framework.
     * These parameters are handled specially and cannot be used as field filter names.
     * If an entity has a field with one of these names, it cannot be filtered via query parameters.
     */
    private static final Set<String> RESERVED_PARAMS = Set.of("sort", "page", "size", "namedQuery");

    @Context
    ResourceInfo resourceInfo;

    @Inject
    Instance<SecurityIdentity> securityIdentity;

    @Inject
    Instance<RestDataPanacheConfig> config;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        SortQueryParamValidator annotation = getAnnotation();
        Set<String> allowedFields = getAllowedFields(annotation);
        Map<String, String[]> secureFields = getSecureFields(annotation);
        MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();

        Response pageSizeError = validatePageSize(queryParams);
        if (pageSizeError != null) {
            requestContext.abortWith(pageSizeError);
            return;
        }

        Response sortError = validateSortParams(queryParams, allowedFields, secureFields);
        if (sortError != null) {
            requestContext.abortWith(sortError);
            return;
        }

        Response filterError = validateFilterParams(queryParams, allowedFields, secureFields);
        if (filterError != null) {
            requestContext.abortWith(filterError);
            return;
        }
    }

    private Response validatePageSize(MultivaluedMap<String, String> queryParams) {
        if (!config.isResolvable()) {
            LOG.error("RestDataPanacheConfig is not resolvable - this indicates a configuration error");
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity("Internal server error")
                    .build();
        }

        RestDataPanacheConfig cfg = config.get();
        if (cfg.maxPageSize().isEmpty()) {
            LOG.error("maxPageSize configuration is not present - this should have a default value");
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity("Internal server error")
                    .build();
        }

        int maxPageSize = cfg.maxPageSize().getAsInt();
        String sizeParam = queryParams.getFirst("size");

        if (sizeParam != null) {
            try {
                int requestedSize = Integer.parseInt(sizeParam);
                if (requestedSize > maxPageSize) {
                    return Response.status(BAD_REQUEST)
                            .entity(String.format("Page size %d exceeds maximum allowed size of %d",
                                    requestedSize, maxPageSize))
                            .build();
                }
            } catch (NumberFormatException e) {
                // Let JAX-RS handle invalid number format
            }
        }

        return null;
    }

    private Response validateSortParams(MultivaluedMap<String, String> queryParams,
            Set<String> allowedFields, Map<String, String[]> secureFields) {
        for (String sort : queryParams.getOrDefault("sort", Collections.emptyList())) {
            for (String sortPart : sort.split(",")) {
                String trimmed = sortPart.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!trimmed.matches(SORT_REGEX)) {
                    return Response.status(BAD_REQUEST)
                            .entity(String.format("Invalid sort parameter '%s'", sort))
                            .build();
                }

                String fieldName = trimmed.startsWith("-") ? trimmed.substring(1) : trimmed;

                if (!allowedFields.contains(fieldName)) {
                    return Response.status(BAD_REQUEST)
                            .entity(String.format("Unknown sort field '%s'", fieldName))
                            .build();
                }

                String[] requiredRoles = secureFields.get(fieldName);
                if (requiredRoles != null) {
                    Response errorResponse = validateSecureField(fieldName, requiredRoles, Operation.SORT);
                    if (errorResponse != null) {
                        return errorResponse;
                    }
                }
            }
        }
        return null;
    }

    private Response validateFilterParams(MultivaluedMap<String, String> queryParams,
            Set<String> allowedFields, Map<String, String[]> secureFields) {
        for (String paramName : queryParams.keySet()) {
            if (RESERVED_PARAMS.contains(paramName)) {
                continue;
            }

            // Validate against allowed fields whitelist (same as sort validation).
            // Even though JAX-RS only binds declared @QueryParam parameters, the full
            // request URI is visible via getUriInfo().getQueryParameters(), so we must
            // reject unknown field names explicitly.
            if (!allowedFields.contains(paramName)) {
                return Response.status(BAD_REQUEST)
                        .entity(String.format("Unknown filter field '%s'", paramName))
                        .build();
            }

            String[] requiredRoles = secureFields.get(paramName);
            if (requiredRoles != null) {
                String value = queryParams.getFirst(paramName);
                if (value != null) {
                    Response errorResponse = validateSecureField(paramName, requiredRoles, Operation.FILTER);
                    if (errorResponse != null) {
                        return errorResponse;
                    }
                }
            }
        }
        return null;
    }

    private Response validateSecureField(String fieldName, String[] requiredRoles, Operation operation) {
        if (!securityIdentity.isResolvable()) {
            LOG.debugf("%s by secure field '%s' denied: SecurityIdentity not resolvable",
                    operation.displayName, fieldName);
            return Response.status(BAD_REQUEST)
                    .entity(String.format("Invalid %s field '%s'",
                            operation.queryLabel, fieldName))
                    .build();
        }

        SecurityIdentity identity = securityIdentity.get();
        if (identity.isAnonymous()) {
            LOG.debugf("%s by secure field '%s' denied: user is anonymous",
                    operation.displayName, fieldName);
            return Response.status(BAD_REQUEST)
                    .entity(String.format("Invalid %s field '%s'",
                            operation.queryLabel, fieldName))
                    .build();
        }

        for (String role : requiredRoles) {
            if (identity.hasRole(role)) {
                return null;
            }
        }

        LOG.debugf("%s by secure field '%s' denied: user lacks required roles",
                operation.displayName, fieldName);
        return Response.status(BAD_REQUEST)
                .entity(String.format("Invalid %s field '%s'",
                        operation.queryLabel, fieldName))
                .build();
    }

    private SortQueryParamValidator getAnnotation() {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return null;
        }
        return method.getAnnotation(SortQueryParamValidator.class);
    }

    private Set<String> getAllowedFields(SortQueryParamValidator annotation) {
        if (annotation == null) {
            return Collections.emptySet();
        }

        String[] fields = annotation.allowedFields();
        if (fields.length == 0) {
            return Collections.emptySet();
        }

        Set<String> allowedFields = new HashSet<>(fields.length);
        Collections.addAll(allowedFields, fields);
        return allowedFields;
    }

    private Map<String, String[]> getSecureFields(SortQueryParamValidator annotation) {
        if (annotation == null) {
            return Collections.emptyMap();
        }

        String[] secureFieldSpecs = annotation.secureFields();
        if (secureFieldSpecs.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, String[]> secureFields = new HashMap<>(secureFieldSpecs.length);
        for (String spec : secureFieldSpecs) {
            int colonIndex = spec.indexOf(':');
            if (colonIndex <= 0 || colonIndex >= spec.length() - 1) {
                throw new IllegalStateException(
                        "Invalid secure field specification '" + spec + "': expected format 'fieldName:role1,role2'");
            }
            String fieldName = spec.substring(0, colonIndex);
            String[] roles = spec.substring(colonIndex + 1).split(",");
            secureFields.put(fieldName, roles);
        }
        return secureFields;
    }

    private enum Operation {
        SORT("Sorting", "sort"),
        FILTER("Filtering", "filter");

        final String displayName;
        final String queryLabel;

        Operation(String displayName, String queryLabel) {
            this.displayName = displayName;
            this.queryLabel = queryLabel;
        }
    }
}
