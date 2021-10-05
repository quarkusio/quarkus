package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.doLegacyWrite;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.setNecessaryJsonFactoryConfig;
import static org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageServerBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

public class FullyFeaturedServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final ObjectMapper originalMapper;
    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<Method, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();

    @Inject
    public FullyFeaturedServerJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.originalMapper = mapper;
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
            // First test the names to see if JsonView is used. We do this to avoid doing reflection for the common case
            // where JsonView is not used
            ResteasyReactiveResourceInfo resourceInfo = context.getResteasyReactiveResourceInfo();
            if (resourceInfo != null) {
                if (resourceInfo.requiresCustomSerialization()) {
                    Method method = resourceInfo.getMethod();
                    if (handleCustomSerialization(method, o, genericType, stream)) {
                        return;
                    }
                } else if (resourceInfo.requiresJsonViewName()) {
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

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, defaultWriter);
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
