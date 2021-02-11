package org.jboss.resteasy.reactive.server.handlers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.serialization.DynamicEntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.EntityWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Our job is to write a Response
 */
public class ResponseWriterHandler implements ServerRestHandler {

    public static final String HEAD = "HEAD";
    private final DynamicEntityWriter dynamicEntityWriter;

    public ResponseWriterHandler(DynamicEntityWriter dynamicEntityWriter) {
        this.dynamicEntityWriter = dynamicEntityWriter;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
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
            ServerSerialisers.encodeResponseHeaders(requestContext);
            requestContext.serverResponse().end();
        }
    }

    // set the content type header to what the resource method uses as a final fallback
    private void setContentTypeIfNecessary(ResteasyReactiveRequestContext requestContext) {
        if (hasBody(requestContext)
                && requestContext.getTarget() != null
                && requestContext.getTarget().getProduces() != null
                && requestContext.getResponseContentType() == null) {
            ServerMediaType serverMediaType = requestContext.getTarget().getProduces();
            if (serverMediaType.getSortedOriginalMediaTypes().length > 0) {
                requestContext.serverResponse().setResponseHeader(HttpHeaders.CONTENT_TYPE,
                        serverMediaType.getSortedOriginalMediaTypes()[0].toString());
            }
        }
    }

    private boolean hasBody(ResteasyReactiveRequestContext requestContext) {
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
