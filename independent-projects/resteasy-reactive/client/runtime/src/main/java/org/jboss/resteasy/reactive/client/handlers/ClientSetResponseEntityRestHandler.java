package org.jboss.resteasy.reactive.client.handlers;

import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.ClientResponseContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.StatusTypeImpl;

public class ClientSetResponseEntityRestHandler implements ClientRestHandler {

    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        ClientRequestContextImpl requestContext = context.getClientRequestContext();
        if (context.isCheckSuccessfulFamily()) {
            StatusType effectiveResponseStatus = determineEffectiveResponseStatus(context, requestContext);
            if (Response.Status.Family.familyOf(effectiveResponseStatus.getStatusCode()) != Response.Status.Family.SUCCESSFUL) {
                throw new WebClientApplicationException(effectiveResponseStatus.getStatusCode(),
                        effectiveResponseStatus.getReasonPhrase());
            }
        }

        // the spec doesn't really say this, but the TCK checks that the abortWith entity ends up read
        // so we have to write it, but without filters/interceptors
        if (isAbortedWith(requestContext)) {
            propagateAbortedWithEntityToResponse(context);
        }
    }

    private StatusType determineEffectiveResponseStatus(RestClientRequestContext context,
            ClientRequestContextImpl requestContext) {
        StatusType effectiveResponseStatus = new StatusTypeImpl(context.getResponseStatus(), context.getResponseReasonPhrase());
        if (effectiveResponseStatus.getStatusCode() == 0) {
            if (isAbortedWith(requestContext)) {
                Response abortedWith = requestContext.getAbortedWith();
                if (abortedWith.getStatusInfo() != null) {
                    effectiveResponseStatus = abortedWith.getStatusInfo();
                }
            }
        }
        return effectiveResponseStatus;
    }

    private boolean isAbortedWith(ClientRequestContextImpl requestContext) {
        return requestContext != null && requestContext.getAbortedWith() != null;
    }

    private void propagateAbortedWithEntityToResponse(RestClientRequestContext restClientRequestContext) throws IOException {
        new ClientResponseContextImpl(restClientRequestContext)
                .setEntityStream(entityStreamOfAbortedResponseOf(restClientRequestContext));
    }

    private ByteArrayInputStream entityStreamOfAbortedResponseOf(RestClientRequestContext context) throws IOException {
        Response abortedWith = context.getAbortedWith();
        Object untypedEntity = abortedWith.getEntity();
        if (untypedEntity == null) {
            return null;
        }

        Entity entity;
        if (untypedEntity instanceof Entity) {
            entity = (Entity) untypedEntity;
        } else {
            MediaType mediaType = abortedWith.getMediaType();
            if (mediaType == null) {
                // FIXME: surely this is wrong, perhaps we can use the expected response type?
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            entity = Entity.entity(untypedEntity, mediaType);
        }
        // FIXME: pass headers?
        Buffer buffer = context.writeEntity(entity, (MultivaluedMap) Serialisers.EMPTY_MULTI_MAP,
                Serialisers.NO_WRITER_INTERCEPTOR);
        return new ByteArrayInputStream(buffer.getBytes());
    }
}
