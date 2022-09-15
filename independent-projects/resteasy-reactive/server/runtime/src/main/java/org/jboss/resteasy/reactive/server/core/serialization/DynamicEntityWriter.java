package org.jboss.resteasy.reactive.server.core.serialization;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.util.List;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.server.core.EncodedMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

/**
 * Writer that is fully dynamic, and follows the spec defined resolution process
 */
public class DynamicEntityWriter implements EntityWriter {

    private static final MessageBodyWriter<?>[] EMPTY_ARRAY = new MessageBodyWriter[0];

    private final ServerSerialisers serialisers;

    public DynamicEntityWriter(ServerSerialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void write(ResteasyReactiveRequestContext context, Object entity) throws IOException {
        EncodedMediaType producesMediaType = context.getResponseContentType();
        MessageBodyWriter<?>[] writers = null;
        MediaType serverSerializersMediaType = null;
        if (producesMediaType == null) {
            MediaType selectedMediaType = null;
            boolean mediaTypeComesFromClient = false;
            ServerHttpRequest vertxRequest = context.serverRequest();
            // first check and see if the resource method defined a media type and try to use it
            if ((context.getTarget() != null) && (context.getTarget().getProduces() != null)) {
                MediaType negotiatedMediaType = context.getTarget().getProduces()
                        .negotiateProduces(vertxRequest.getRequestHeader(HttpHeaders.ACCEPT)).getKey();
                List<MessageBodyWriter<?>> writersList = serialisers.findWriters(null, entity.getClass(), negotiatedMediaType,
                        RuntimeType.SERVER);
                if (!writersList.isEmpty()) {
                    writers = writersList.toArray(EMPTY_ARRAY);
                    // use the actual type the method declares as this is what the spec expects despite the fact that we might
                    // have used the suffix of the subtype to determine a MessageBodyWriter
                    MediaType[] sortedOriginalMediaTypes = context.getTarget().getProduces().getSortedOriginalMediaTypes();
                    for (MediaType methodMediaType : sortedOriginalMediaTypes) {
                        if (methodMediaType.isCompatible(negotiatedMediaType)) {
                            selectedMediaType = methodMediaType;
                            break;
                        }
                    }
                    if (selectedMediaType == null) {
                        // this should never happen
                        selectedMediaType = sortedOriginalMediaTypes[0];
                    }
                }
            } else if (vertxRequest.getRequestHeader(HttpHeaders.ACCEPT) != null
                    && !MediaType.WILDCARD.equals(vertxRequest.getRequestHeader(HttpHeaders.ACCEPT))) {
                // try and find a writer based on the 'Accept' header match

                ServerSerialisers.BestMatchingServerWriterResult bestMatchingServerWriterResult = serialisers
                        .findBestMatchingServerWriter(null, entity.getClass(), vertxRequest);
                if (!bestMatchingServerWriterResult.isEmpty()) {
                    selectedMediaType = bestMatchingServerWriterResult.getSelectedMediaType();
                    mediaTypeComesFromClient = true;
                    writers = bestMatchingServerWriterResult.getMessageBodyWriters().toArray(ServerSerialisers.NO_WRITER);
                }
            }
            // try to find a Writer based on the entity type
            if (writers == null) {
                ServerSerialisers.NoMediaTypeResult writerNoMediaType = serialisers.findWriterNoMediaType(context, entity,
                        serialisers, RuntimeType.SERVER);
                writers = writerNoMediaType.getWriters();
                selectedMediaType = writerNoMediaType.getMediaType();
            }
            if (selectedMediaType != null) {
                if (MediaTypeHelper.isUnsupportedWildcardSubtype(selectedMediaType) && !mediaTypeComesFromClient) { // spec says the acceptable wildcard subtypes are */* or application/*
                    ServerSerialisers.encodeResponseHeaders(context);
                    // set the response header AFTER encodeResponseHeaders in order to override what Response has as we want this to be the final result
                    ServerHttpResponse httpServerResponse = context.serverResponse();
                    httpServerResponse.setStatusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
                    // spec says the response doesn't have a body so we just end the response here and return
                    httpServerResponse.end();
                    return;
                } else {
                    serverSerializersMediaType = selectedMediaType;
                    context.setResponseContentType(selectedMediaType);
                    // this will be used as the fallback if Response does NOT contain a type
                    context.serverResponse().addResponseHeader(HttpHeaders.CONTENT_TYPE,
                            context.getResponseContentType().toString());
                }
            }
        } else {
            writers = serialisers
                    .findWriters(null, entity.getClass(), producesMediaType.getMediaType(), RuntimeType.SERVER)
                    .toArray(ServerSerialisers.NO_WRITER);
        }
        for (MessageBodyWriter<?> w : writers) {
            if (ServerSerialisers.invokeWriter(context, entity, w, serialisers, serverSerializersMediaType)) {
                return;
            }
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }

}
