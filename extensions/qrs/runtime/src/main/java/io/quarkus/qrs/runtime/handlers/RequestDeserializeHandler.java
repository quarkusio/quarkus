package io.quarkus.qrs.runtime.handlers;

import java.io.ByteArrayInputStream;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.qrs.runtime.core.RequestContext;
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
    public void handle(RequestContext requestContext) throws Exception {
        MessageBodyReader<?> reader = serialisers.findReader(type, mediaType, requestContext);
        if (reader == null) {
            requestContext.setThrowable(new NotSupportedException());
            return;
        }
        requestContext.suspend();
        //TODO: size limits
        requestContext.getContext().request().bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                ByteArrayInputStream in = new ByteArrayInputStream(event.getBytes());
                try {
                    Object result = reader.readFrom((Class) type, type, null, mediaType,
                            requestContext.getHttpHeaders().getRequestHeaders(), in);
                    requestContext.setRequestEntity(result);
                    requestContext.resume();
                } catch (Throwable e) {
                    requestContext.setThrowable(e);
                    requestContext.resume();
                    return;
                }

            }
        });

    }
}
