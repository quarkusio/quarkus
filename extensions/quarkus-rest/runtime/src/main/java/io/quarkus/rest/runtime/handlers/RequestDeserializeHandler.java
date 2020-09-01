package io.quarkus.rest.runtime.handlers;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;

public class RequestDeserializeHandler implements RestHandler {

    private final Class<?> type;
    private final MediaType mediaType;
    private final Serialisers serialisers;

    public RequestDeserializeHandler(Class<?> type, MediaType mediaType, Serialisers serialisers) {
        this.type = type;
        this.mediaType = mediaType;
        this.serialisers = serialisers;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        MediaType requestType = mediaType;
        String requestTypeString = requestContext.getContext().request().getHeader(HttpHeaders.CONTENT_TYPE);
        if (requestTypeString != null) {
            try {
                requestType = MediaType.valueOf(requestTypeString);
            } catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
        }
        List<MessageBodyReader<?>> readers = serialisers.findReaders(type, requestType);
        if (readers.isEmpty()) {
            throw new NotSupportedException();
        }
        InputStream in = requestContext.getInputStream();
        for (MessageBodyReader<?> reader : readers) {
            //TODO: proper params
            if (reader.isReadable(type, type, requestContext.getAnnotations(), mediaType)) {
                Object result = reader.readFrom((Class) type, type, null, mediaType,
                        requestContext.getHttpHeaders().getRequestHeaders(), in);
                requestContext.setRequestEntity(result);
                requestContext.resume();
                return;
            }
        }
        throw new NotSupportedException();
    }
}
