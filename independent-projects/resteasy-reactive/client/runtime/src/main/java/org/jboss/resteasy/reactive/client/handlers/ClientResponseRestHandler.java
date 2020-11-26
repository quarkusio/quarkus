package org.jboss.resteasy.reactive.client.handlers;

import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.api.WebClientApplicationException;
import org.jboss.resteasy.reactive.client.QuarkusRestClientRequestContext;
import org.jboss.resteasy.reactive.client.QuarkusRestClientResponseBuilder;
import org.jboss.resteasy.reactive.client.QuarkusRestClientResponseContext;
import org.jboss.resteasy.reactive.client.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;

public class ClientResponseRestHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        QuarkusRestClientResponseContext responseContext = new QuarkusRestClientResponseContext(context);
        if (context.isCheckSuccessfulFamily()
                && (responseContext.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)) {
            throw new WebClientApplicationException("Server response status was: " + responseContext.getStatus());
        }
        QuarkusRestClientRequestContext requestContext = context.getClientRequestContext();
        // the spec doesn't really say this, but the TCK checks that the abortWith entity ends up read
        // so we have to write it, but without filters/interceptors
        if (requestContext != null && requestContext.getAbortedWith() != null) {
            setExistingEntity(requestContext.getAbortedWith(), responseContext, context);
        }

        List<ClientResponseFilter> filters = context.getConfiguration().getResponseFilters();
        if (!filters.isEmpty()) {
            // FIXME: pretty sure we'll have to mark it as immutable in this phase, but the spec is not verbose about this
            // the server does it.
            for (ClientResponseFilter filter : filters) {
                try {
                    filter.filter(requestContext, responseContext);
                } catch (Exception x) {
                    throw new ProcessingException(x);
                }
            }
        }
        QuarkusRestClientResponseBuilder builder = new QuarkusRestClientResponseBuilder();
        builder.status(responseContext.getStatus(), responseContext.getReasonPhrase());
        builder.setAllHeaders(responseContext.getHeaders());
        builder.invocationState(context);
        if (context.isResponseTypeSpecified()) { // this case means that a specific response type was requested
            Object entity = context.readEntity(responseContext.getEntityStream(),
                    context.getResponseType(),
                    responseContext.getMediaType(),
                    // FIXME: we have strings, it wants objects, perhaps there's
                    // an Object->String conversion too many
                    (MultivaluedMap) responseContext.getHeaders());
            if (entity != null) {
                builder.entity(entity);
            }
        } else {
            // in this case no specific response type was requested so we just prepare the stream
            // the users of the response are meant to use readEntity
            builder.entityStream(responseContext.getEntityStream());
        }
        context.getResult().complete(builder.build());
    }

    private void setExistingEntity(Response abortedWith, QuarkusRestClientResponseContext responseContext,
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
