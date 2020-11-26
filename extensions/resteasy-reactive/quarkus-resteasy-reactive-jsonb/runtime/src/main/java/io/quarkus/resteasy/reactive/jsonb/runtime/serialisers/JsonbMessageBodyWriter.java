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

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;

public class JsonbMessageBodyWriter implements ResteasyReactiveMessageBodyWriter<Object> {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyWriter(Jsonb json) {
        this.json = json;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(((String) o).getBytes());
        } else {
            json.toJson(o, type, entityStream);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, ResteasyReactiveRequestContext context) throws WebApplicationException, IOException {
        try (OutputStream stream = context.getOrCreateOutputStream()) {
            if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
                stream.write(((String) o).getBytes());
            } else {
                json.toJson(o, stream);
            }
        }

    }
}
