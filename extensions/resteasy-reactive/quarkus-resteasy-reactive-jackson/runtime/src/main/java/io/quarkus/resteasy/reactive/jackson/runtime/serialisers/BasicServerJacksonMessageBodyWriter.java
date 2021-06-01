package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.doLegacyWrite;
import static org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageServerBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class BasicServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final ObjectWriter defaultWriter;

    @Inject
    public BasicServerJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.defaultWriter = createDefaultWriter(mapper);
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        setContentTypeIfNecessary(context);
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes());
        } else {
            defaultWriter.writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, defaultWriter);
    }

}
