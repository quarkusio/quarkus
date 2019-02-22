package io.quarkus.resteasy.runtime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Helper class for JAXRS request failure responses.
 */
public class RequestFailer {

    public static void fail(ContainerRequestContext requestContext) {
        if (requestContext.getSecurityContext().getUserPrincipal() == null) {
            respond(requestContext, 401, "Not authorized");
        } else {
            respond(requestContext, 403, "Access forbidden: role not allowed");
        }
    }

    private static void respond(ContainerRequestContext context, int status, String message) {
        Response response = Response.status(status)
                .entity(message)
                .type("text/html;charset=UTF-8")
                .build();
        context.abortWith(response);
    }
}
