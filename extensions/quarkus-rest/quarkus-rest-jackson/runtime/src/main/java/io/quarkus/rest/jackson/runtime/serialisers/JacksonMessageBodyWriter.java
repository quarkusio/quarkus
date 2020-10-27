package io.quarkus.rest.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;

public class JacksonMessageBodyWriter implements QuarkusRestMessageBodyWriter<Object> {

    private final ObjectMapper mapper;

    @Inject
    public JacksonMessageBodyWriter(ObjectMapper mapper) {
        this.mapper = mapper;
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
            entityStream.write(mapper.writeValueAsBytes(o));
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, QuarkusRestRequestContext context) throws WebApplicationException, IOException {
        try (OutputStream stream = context.getOrCreateOutputStream()) {
            if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
                stream.write(((String) o).getBytes());
            } else {
                mapper.writeValue(stream, o);
            }
        }
    }
}
