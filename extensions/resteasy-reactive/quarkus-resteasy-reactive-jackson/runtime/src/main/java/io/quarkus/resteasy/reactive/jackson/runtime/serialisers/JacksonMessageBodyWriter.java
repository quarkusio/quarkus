package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonMessageBodyWriter implements ServerMessageBodyWriter<Object> {

    private static final String JSON_VIEW_NAME = JsonView.class.getName();
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
    public boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, ServerRequestContext context) throws WebApplicationException, IOException {
        try (OutputStream stream = context.getOrCreateOutputStream()) {
            if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
                stream.write(((String) o).getBytes());
            } else {
                // First test the names to see if JsonView is used. We do this to avoid doing reflection for the common case
                // where JsonView is not used
                if (context.getResteasyReactiveResourceInfo().getMethodAnnotationNames().contains(JSON_VIEW_NAME)) {
                    Method method = context.getResteasyReactiveResourceInfo().getMethod();
                    JsonView jsonView = method.getAnnotation(JsonView.class);
                    if ((jsonView != null) && (jsonView.value().length > 0)) {
                        mapper.writerWithView(jsonView.value()[0]).writeValue(stream, o);
                        return;
                    }
                }
                mapper.writeValue(stream, o);
            }
        }
    }
}
