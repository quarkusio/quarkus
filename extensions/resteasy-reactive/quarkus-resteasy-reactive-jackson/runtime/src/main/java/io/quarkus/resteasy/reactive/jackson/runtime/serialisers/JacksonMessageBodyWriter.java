package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageBodyWriterUtil.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
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

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

public class JacksonMessageBodyWriter implements ServerMessageBodyWriter<Object> {

    private static final String JSON_VIEW_NAME = JsonView.class.getName();
    private static final String CUSTOM_SERIALIZATION = CustomSerialization.class.getName();

    private final ObjectMapper originalMapper;
    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<Method, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();

    @Inject
    public JacksonMessageBodyWriter(ObjectMapper mapper) {
        this.originalMapper = mapper;
        // we don't want the ObjectWriter to close the stream automatically, as we want to handle closing manually at the proper points
        if (mapper.getFactory().isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
            JsonFactory jsonFactory = mapper.getFactory().copy();
            jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
            this.defaultWriter = mapper.writer().with(jsonFactory);
        } else {
            this.defaultWriter = mapper.writer();
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        setContentTypeIfNecessary(httpHeaders);
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(((String) o).getBytes());
        } else {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (JsonView.class.equals(annotation.annotationType())) {
                        if (handleJsonView(((JsonView) annotation), o, entityStream)) {
                            return;
                        }
                    }
                }
            }
            entityStream.write(defaultWriter.writeValueAsBytes(o));
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        setContentTypeIfNecessary(context);
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes());
        } else {
            // First test the names to see if JsonView is used. We do this to avoid doing reflection for the common case
            // where JsonView is not used
            ResteasyReactiveResourceInfo resourceInfo = context.getResteasyReactiveResourceInfo();
            if (resourceInfo != null) {
                Set<String> methodAnnotationNames = resourceInfo.getMethodAnnotationNames();
                if (methodAnnotationNames.contains(CUSTOM_SERIALIZATION)) {
                    Method method = resourceInfo.getMethod();
                    if (handleCustomSerialization(method, o, genericType, stream)) {
                        return;
                    }
                } else if (methodAnnotationNames.contains(JSON_VIEW_NAME)) {
                    Method method = resourceInfo.getMethod();
                    if (handleJsonView(method.getAnnotation(JsonView.class), o, stream)) {
                        return;
                    }
                }
            }
            defaultWriter.writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    // TODO: this can definitely be made faster if necessary by optimizing the use of the map and also by moving the creation of the
    //  biFunction to build time
    private boolean handleCustomSerialization(Method method, Object o, Type genericType, OutputStream stream)
            throws IOException {
        CustomSerialization customSerialization = method.getAnnotation(CustomSerialization.class);
        if ((customSerialization == null)) {
            return false;
        }
        Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> biFunctionClass = customSerialization.value();
        ObjectWriter objectWriter = perMethodWriter.computeIfAbsent(method,
                new MethodObjectWriterFunction(biFunctionClass, genericType, originalMapper));
        objectWriter.writeValue(stream, o);
        return true;
    }

    private boolean handleJsonView(JsonView jsonView, Object o, OutputStream stream) throws IOException {
        if ((jsonView != null) && (jsonView.value().length > 0)) {
            defaultWriter.withView(jsonView.value()[0]).writeValue(stream, o);
            return true;
        }
        return false;
    }

    private static class MethodObjectWriterFunction implements Function<Method, ObjectWriter> {
        private final Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> clazz;
        private final Type genericType;
        private final ObjectMapper originalMapper;

        public MethodObjectWriterFunction(Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> clazz, Type genericType,
                ObjectMapper originalMapper) {
            this.clazz = clazz;
            this.genericType = genericType;
            this.originalMapper = originalMapper;
        }

        @Override
        public ObjectWriter apply(Method method) {
            try {
                BiFunction<ObjectMapper, Type, ObjectWriter> biFunctionInstance = clazz.getDeclaredConstructor().newInstance();
                return biFunctionInstance.apply(originalMapper, genericType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
