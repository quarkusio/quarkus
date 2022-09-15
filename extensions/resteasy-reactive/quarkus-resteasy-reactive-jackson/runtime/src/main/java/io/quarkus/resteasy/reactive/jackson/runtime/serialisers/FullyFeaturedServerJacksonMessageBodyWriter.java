package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.setNecessaryJsonFactoryConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;

public class FullyFeaturedServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final ObjectMapper originalMapper;
    private final Providers providers;
    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<String, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();
    private final ConcurrentMap<ObjectMapper, ObjectWriter> contextResolverMap = new ConcurrentHashMap<>();

    @Inject
    public FullyFeaturedServerJacksonMessageBodyWriter(ObjectMapper mapper, Providers providers) {
        this.originalMapper = mapper;
        this.defaultWriter = createDefaultWriter(mapper);
        this.providers = providers;
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            ObjectMapper effectiveMapper = getEffectiveMapper(o, context);

            // First test the names to see if JsonView is used. We do this to avoid doing reflection for the common case
            // where JsonView is not used
            ResteasyReactiveResourceInfo resourceInfo = context.getResteasyReactiveResourceInfo();
            if (resourceInfo != null) {
                String methodId = resourceInfo.getMethodId();
                var customSerializationValue = ResteasyReactiveServerJacksonRecorder.customSerializationForMethod(methodId);
                if (customSerializationValue != null) {
                    ObjectWriter objectWriter = perMethodWriter.computeIfAbsent(methodId,
                            new MethodObjectWriterFunction(customSerializationValue, genericType, effectiveMapper));
                    objectWriter.writeValue(stream, o);
                    return;
                }

                Class<?> jsonViewValue = ResteasyReactiveServerJacksonRecorder.jsonViewForMethod(methodId);
                if (jsonViewValue != null) {
                    getEffectiveWriter(effectiveMapper).withView(jsonViewValue).writeValue(stream, o);
                    return;
                }
            }
            getEffectiveWriter(effectiveMapper).writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    private ObjectWriter getEffectiveWriter(ObjectMapper effectiveMapper) {
        if (effectiveMapper == originalMapper) {
            return defaultWriter;
        }
        return contextResolverMap.computeIfAbsent(effectiveMapper, new Function<>() {
            @Override
            public ObjectWriter apply(ObjectMapper objectMapper) {
                return createDefaultWriter(effectiveMapper);
            }
        });
    }

    /**
     * Obtains the user configured {@link ObjectMapper} if there is a {@link ContextResolver} configured.
     * Otherwise, returns the default {@link ObjectMapper}.
     */
    private ObjectMapper getEffectiveMapper(Object o, ServerRequestContext context) {
        ObjectMapper effectiveMapper = originalMapper;
        ContextResolver<ObjectMapper> contextResolver = providers.getContextResolver(ObjectMapper.class,
                context.getResponseMediaType());
        if (contextResolver == null) {
            // TODO: not sure if this is correct, but Jackson does this as well...
            contextResolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        if (contextResolver != null) {
            ObjectMapper mapperFromContextResolver = contextResolver.getContext(o.getClass());
            if (mapperFromContextResolver != null) {
                effectiveMapper = mapperFromContextResolver;
            }
        }
        return effectiveMapper;
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
