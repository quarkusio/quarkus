package io.quarkus.resteasy.reactive.jsonb.runtime.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageServerBodyWriterUtil;

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
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(((String) o).getBytes());
        } else {
            json.toJson(o, type, entityStream);
        }
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        JsonMessageServerBodyWriterUtil.setContentTypeIfNecessary(context);
        OutputStream originalStream = context.getOrCreateOutputStream();
        OutputStream stream = new NoopCloseAndFlushOutputStream(originalStream);
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes());
        } else {
            json.toJson(o, stream);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        originalStream.close();
    }

    /**
     * This class is needed because Yasson doesn't give us a way to control if the output stream is going to be closed or not
     */
    private static class NoopCloseAndFlushOutputStream extends OutputStream {
        private final OutputStream delegate;

        public NoopCloseAndFlushOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }
    }
}
