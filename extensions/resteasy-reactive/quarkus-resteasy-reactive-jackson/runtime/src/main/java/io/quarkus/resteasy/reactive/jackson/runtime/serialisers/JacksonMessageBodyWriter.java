package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageServerBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

public class JacksonMessageBodyWriter extends JacksonBasicMessageBodyWriter implements ServerMessageBodyWriter<Object> {// extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private static final String JSON_VIEW_NAME = JsonView.class.getName();
    private static final String CUSTOM_SERIALIZATION = CustomSerialization.class.getName();

    @Inject
    public JacksonMessageBodyWriter(ObjectMapper mapper) {
        super(mapper);
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

    @Override
    public final boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
            MediaType mediaType) {
        return true;
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
                ObjectWriter objectWriter = biFunctionInstance.apply(originalMapper, genericType);
                setNecessaryJsonFactoryConfig(objectWriter.getFactory());
                return objectWriter;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
