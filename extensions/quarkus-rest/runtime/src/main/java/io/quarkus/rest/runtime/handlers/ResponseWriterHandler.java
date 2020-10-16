package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.util.ServerMediaType;

/**
 * Our job is to write a Response
 */
public class ResponseWriterHandler implements RestHandler {

    public static final String HEAD = "HEAD";
    private final DynamicEntityWriter dynamicEntityWriter;

    public ResponseWriterHandler(DynamicEntityWriter dynamicEntityWriter) {
        this.dynamicEntityWriter = dynamicEntityWriter;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        Object entity = requestContext.getResponseEntity();
        if (entity != null && !requestContext.getMethod().equals(HEAD)) {
            EntityWriter entityWriter = requestContext.getEntityWriter();
            if (entityWriter == null) {
                dynamicEntityWriter.write(requestContext, entity);
            } else {
                entityWriter.write(requestContext, entity);
            }
        } else {
            setContentTypeIfNecessary(requestContext);
            Serialisers.encodeResponseHeaders(requestContext);
            requestContext.getContext().response().end();
        }
    }

    // set the content type header to what the resource method uses as a final fallback
    private void setContentTypeIfNecessary(QuarkusRestRequestContext requestContext) {
        if (hasBody(requestContext)
                && requestContext.getTarget() != null
                && requestContext.getTarget().getProduces() != null
                && requestContext.getResponseContentType() == null) {
            ServerMediaType serverMediaType = requestContext.getTarget().getProduces();
            if (serverMediaType.getSortedOriginalMediaTypes().length > 0) {
                requestContext.getContext().response().headers().add(HttpHeaders.CONTENT_TYPE,
                        serverMediaType.getSortedOriginalMediaTypes()[0].toString());
            }
        }
    }

    private boolean hasBody(QuarkusRestRequestContext requestContext) {
        // pretend it has, because we want content-type/length headers
        if (requestContext.getMethod().equals(HEAD))
            return true;
        if (requestContext.getResponse().isCreated()) {
            int status = requestContext.getResponse().get().getStatus();
            return status != Response.Status.NO_CONTENT.getStatusCode();
        } else {
            return requestContext.getResponseEntity() != null;
        }
    }
}
