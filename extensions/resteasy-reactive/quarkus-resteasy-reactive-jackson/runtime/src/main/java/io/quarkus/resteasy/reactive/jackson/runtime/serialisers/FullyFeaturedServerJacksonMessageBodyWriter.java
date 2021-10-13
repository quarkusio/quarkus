package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.doLegacyWrite;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.setNecessaryJsonFactoryConfig;
import static org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json.JsonMessageServerBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;

public class FullyFeaturedServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final ObjectMapper originalMapper;
    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<String, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();

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
                String methodId = resourceInfo.getMethodId();
                var customSerializationValue = ResteasyReactiveServerJacksonRecorder.customSerializationForMethod(methodId);
                if (customSerializationValue != null) {
                    ObjectWriter objectWriter = perMethodWriter.computeIfAbsent(methodId,
                            new MethodObjectWriterFunction(customSerializationValue, genericType, originalMapper));
                    objectWriter.writeValue(stream, o);
                    return;
                }

                Class<?> jsonViewValue = ResteasyReactiveServerJacksonRecorder.jsonViewForMethod(methodId);
                if (jsonViewValue != null) {
                    defaultWriter.withView(jsonViewValue).writeValue(stream, o);
                    return;
                }
            }
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

    private static class MethodObjectWriterFunction implements Function<String, ObjectWriter> {
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
        public ObjectWriter apply(String methodId) {
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
