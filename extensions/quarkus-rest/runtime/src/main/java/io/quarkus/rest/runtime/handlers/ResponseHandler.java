package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.WebApplicationException;
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

            boolean needsMapping = true;
            // according to the spec, when the exception is a WebApplicationException and it has an entity, no exception mapping takes place
            if (error instanceof WebApplicationException) {
                Response webApplicationExceptionResponse = ((WebApplicationException) error).getResponse();
                if ((webApplicationExceptionResponse != null) && webApplicationExceptionResponse.hasEntity()) {
                    requestContext.setResult(webApplicationExceptionResponse);
                    needsMapping = false;
                }
            }
            if (needsMapping) {
                requestContext.setResult(requestContext.getDeployment().getExceptionMapping().mapException(error));
            }
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
                // TCK says to use the entity type as generic type if we return a response
                if (existing.hasEntity())
                    requestContext.setGenericReturnType(existing.getEntity().getClass());
                //TODO: super inefficent
                response = Response.fromResponse((Response) result);
                if ((result instanceof QuarkusRestResponse)) {
                    // needed in order to preserve entity annotations
                    QuarkusRestResponse quarkusRestResponse = (QuarkusRestResponse) result;
                    if (quarkusRestResponse.getEntityAnnotations() != null) {
                        requestContext.setAdditionalAnnotations(quarkusRestResponse.getEntityAnnotations());
                    }
                }
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
