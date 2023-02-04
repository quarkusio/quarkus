package org.jboss.resteasy.reactive.server.jsonb;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil;
import org.jboss.resteasy.reactive.server.NoopCloseAndFlushOutputStream;
import org.jboss.resteasy.reactive.server.StreamingOutputStream;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class JsonbMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyWriter(Jsonb json) {
        this.json = json;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JsonMessageBodyWriterUtil.setContentTypeIfNecessary(httpHeaders);
        if ((o instanceof String) && (!(entityStream instanceof StreamingOutputStream))) {
            // YUK: done in order to avoid adding extra quotes... when we are not streaming a result
            entityStream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            json.toJson(o, type, entityStream);
        }
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        OutputStream originalStream = context.getOrCreateOutputStream();
        OutputStream stream = new NoopCloseAndFlushOutputStream(originalStream);
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            json.toJson(o, stream);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        originalStream.close();
    }
}
