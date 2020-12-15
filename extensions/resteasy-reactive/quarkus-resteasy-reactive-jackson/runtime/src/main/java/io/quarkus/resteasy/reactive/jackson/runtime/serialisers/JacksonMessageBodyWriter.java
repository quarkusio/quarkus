package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JacksonMessageBodyWriter implements ServerMessageBodyWriter<Object> {

    private static final String JSON_VIEW_NAME = JsonView.class.getName();
    private final ObjectWriter writer;

    @Inject
    public JacksonMessageBodyWriter(ObjectMapper mapper) {
        // we don't want the ObjectWriter to close the stream automatically, as we want to handle closing manually at the proper points
        if (mapper.getFactory().isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
            JsonFactory jsonFactory = mapper.getFactory().copy();
            jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
            this.writer = mapper.writer().with(jsonFactory);
        } else {
            this.writer = mapper.writer();
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(((String) o).getBytes());
        } else {
            entityStream.write(writer.writeValueAsBytes(o));
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, ServerRequestContext context) throws WebApplicationException, IOException {
        context.serverResponse().setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes());
        } else {
            // First test the names to see if JsonView is used. We do this to avoid doing reflection for the common case
            // where JsonView is not used
            if (context.getResteasyReactiveResourceInfo().getMethodAnnotationNames().contains(JSON_VIEW_NAME)) {
                Method method = context.getResteasyReactiveResourceInfo().getMethod();
                JsonView jsonView = method.getAnnotation(JsonView.class);
                if ((jsonView != null) && (jsonView.value().length > 0)) {
                    writer.withView(jsonView.value()[0]).writeValue(stream, o);
                    return;
                }
            }
            writer.writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }
}
