package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

/**
 * Our job is to turn endpoint return types into Response instances
 */
public class ResponseHandler implements RestHandler {

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        Throwable error = requestContext.getThrowable();
        if (error != null) {
            requestContext.setThrowable(null);
            requestContext.setResult(requestContext.getDeployment().getExceptionMapping().mapException(error));
        }
        Object result = requestContext.getResult();
        Response.ResponseBuilder response = null;
        if (result instanceof Response) {
            Response existing = (Response) result;
            if (existing.getEntity() instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) existing.getEntity();
                requestContext.setGenericReturnType(genericEntity.getType());
                response = Response.fromResponse(existing).entity(genericEntity.getEntity());
            } else {
                //TODO: super inefficent
                response = Response.fromResponse((Response) result);
            }
        } else {
            if (result instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) result;
                requestContext.setGenericReturnType(genericEntity.getType());
                response = Response.ok().entity(genericEntity.getEntity());
            } else if (result == null) {
                // FIXME: custom status codes depending on method?
                response = Response.noContent().entity(result);
            } else {
                // FIXME: custom status codes depending on method?
                response = Response.ok().entity(result);
            }
        }
        MediaType produces = requestContext.getProducesMediaType();
        if (produces != null) {
            response.header(HttpHeaders.CONTENT_TYPE, produces.toString());
        }
        requestContext.setResult(response.build());
    }
}
