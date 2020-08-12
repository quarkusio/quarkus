package io.quarkus.qrs.runtime.handlers;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

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
    public void handle(QrsRequestContext requestContext) throws Exception {
        MediaType requestType = mediaType;
        String requestTypeString = requestContext.getContext().request().getHeader(HttpHeaders.CONTENT_TYPE);
        if (requestTypeString != null) {
            try {
                requestType = MediaType.valueOf(requestTypeString);
            } catch (Exception e) {
                requestContext.setThrowable(new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build()));
                return;
            }
        }
        List<MessageBodyReader<?>> readers = serialisers.findReaders(type, requestType);
        if (readers.isEmpty()) {
            requestContext.setThrowable(new NotSupportedException());
            return;
        }
        requestContext.suspend();
        //TODO: size limits
        requestContext.getContext().request().bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                ByteArrayInputStream in = new ByteArrayInputStream(event.getBytes());
                for (MessageBodyReader<?> reader : readers) {
                    //TODO: proper params
                    if (reader.isReadable(type, type, null, mediaType)) {
                        try {
                            Object result = reader.readFrom((Class) type, type, null, mediaType,
                                    requestContext.getHttpHeaders().getRequestHeaders(), in);
                            requestContext.setRequestEntity(result);
                            requestContext.resume();
                            return;
                        } catch (Throwable e) {
                            requestContext.setThrowable(e);
                            requestContext.resume();
                            return;
                        }
                    }
                }
                requestContext.setThrowable(new NotSupportedException());
                requestContext.resume();
            }
        });

    }
}
