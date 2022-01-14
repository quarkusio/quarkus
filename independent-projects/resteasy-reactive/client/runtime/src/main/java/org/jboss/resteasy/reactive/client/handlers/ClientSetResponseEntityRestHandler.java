package org.jboss.resteasy.reactive.client.handlers;

import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.ClientResponseContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;

public class ClientSetResponseEntityRestHandler implements ClientRestHandler {

    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        ClientRequestContextImpl requestContext = context.getClientRequestContext();
        ClientResponseContextImpl responseContext = new ClientResponseContextImpl(context);
        if (context.isCheckSuccessfulFamily()) {
            int effectiveResponseStatus = determineEffectiveResponseStatus(context, requestContext);
            if (Response.Status.Family.familyOf(effectiveResponseStatus) != Response.Status.Family.SUCCESSFUL) {
                throw new WebClientApplicationException(effectiveResponseStatus, context.getResponseReasonPhrase());
            }
        }

        // the spec doesn't really say this, but the TCK checks that the abortWith entity ends up read
        // so we have to write it, but without filters/interceptors
        if (isAbortedWith(requestContext)) {
            setExistingEntity(requestContext.getAbortedWith(), responseContext, context);
        }
    }

    private int determineEffectiveResponseStatus(RestClientRequestContext context, ClientRequestContextImpl requestContext) {
        int effectiveResponseStatus = context.getResponseStatus();
        if (effectiveResponseStatus == 0) {
            if (isAbortedWith(requestContext)) {
                effectiveResponseStatus = requestContext.getAbortedWith().getStatus();
            }
        }
        return effectiveResponseStatus;
    }

    private boolean isAbortedWith(ClientRequestContextImpl requestContext) {
        return requestContext != null && requestContext.getAbortedWith() != null;
    }

    private void setExistingEntity(Response abortedWith, ClientResponseContextImpl responseContext,
            RestClientRequestContext restClientRequestContext) throws IOException {
        Object value = abortedWith.getEntity();
        if (value == null) {
            responseContext.setEntityStream(null);
            return;
        }
        Entity entity;
        if (value instanceof Entity) {
            entity = (Entity) value;
        } else {
            MediaType mediaType = abortedWith.getMediaType();
            if (mediaType == null) {
                // FIXME: surely this is wrong, perhaps we can use the expected response type?
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            entity = Entity.entity(value, mediaType);
        }
        // FIXME: pass headers?
        Buffer buffer = restClientRequestContext.writeEntity(entity, (MultivaluedMap) Serialisers.EMPTY_MULTI_MAP,
                Serialisers.NO_WRITER_INTERCEPTOR);
        responseContext.setEntityStream(new ByteArrayInputStream(buffer.getBytes()));
    }
}
