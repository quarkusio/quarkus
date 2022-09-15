package io.quarkus.rest.data.panache.runtime.sort;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import java.util.Collections;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@SortQueryParamValidator
public class SortQueryParamFilter implements ContainerRequestFilter {

    private static final String SORT_REGEX = "-?([a-z]|[A-Z]|_|\\$|[\u0080-\ufffe])([a-z]|[A-Z]|_|\\$|[0-9]|[\u0080-\ufffe])*";

    /**
     * Verifies that sort query parameters are valid.
     * Valid examples:
     * * ?sort=name,surname
     * * ?sort=$surname&sort=-age
     * * ?sort=_id
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
        for (String sort : queryParams.getOrDefault("sort", Collections.emptyList())) {
            for (String sortPart : sort.split(",")) {
                String trimmed = sortPart.trim();
                if (trimmed.length() > 0 && !trimmed.matches(SORT_REGEX)) {
                    requestContext.abortWith(
                            Response.status(BAD_REQUEST)
                                    .entity(String.format("Invalid sort parameter '%s'", sort))
                                    .build());
                }
            }
        }
    }
}
