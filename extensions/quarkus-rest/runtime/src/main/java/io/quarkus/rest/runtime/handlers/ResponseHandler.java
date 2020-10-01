package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;

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
        Response.ResponseBuilder responseBuilder;
        boolean mediaTypeAlreadyExists = false;
        if (result instanceof Response) {
            Response existing = (Response) result;
            if (existing.getEntity() instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) existing.getEntity();
                requestContext.setGenericReturnType(genericEntity.getType());
                responseBuilder = Response.fromResponse(existing).entity(genericEntity.getEntity());
            } else {
                // TCK says to use the entity type as generic type if we return a response
                if (existing.hasEntity())
                    requestContext.setGenericReturnType(existing.getEntity().getClass());
                //TODO: super inefficent
                responseBuilder = Response.fromResponse((Response) result);
                if ((result instanceof QuarkusRestResponse)) {
                    // needed in order to preserve entity annotations
                    QuarkusRestResponse quarkusRestResponse = (QuarkusRestResponse) result;
                    if (quarkusRestResponse.getEntityAnnotations() != null) {
                        requestContext.setAdditionalAnnotations(quarkusRestResponse.getEntityAnnotations());
                    }
                }
            }
            if (existing.getMediaType() != null) {
                requestContext.setProducesMediaType(existing.getMediaType());
                mediaTypeAlreadyExists = true;
            }
        } else {
            if (result instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) result;
                requestContext.setGenericReturnType(genericEntity.getType());
                responseBuilder = Response.ok().entity(genericEntity.getEntity());
            } else if (result == null) {
                // FIXME: custom status codes depending on method?
                responseBuilder = Response.noContent().entity(result);
            } else {
                // FIXME: custom status codes depending on method?
                responseBuilder = Response.ok().entity(result);
            }
        }
        MediaType produces = requestContext.getProducesMediaType();
        if (!mediaTypeAlreadyExists && produces != null) {
            responseBuilder.header(HttpHeaders.CONTENT_TYPE, produces.toString());
        }
        requestContext.setResult(responseBuilder.build());
    }
}
