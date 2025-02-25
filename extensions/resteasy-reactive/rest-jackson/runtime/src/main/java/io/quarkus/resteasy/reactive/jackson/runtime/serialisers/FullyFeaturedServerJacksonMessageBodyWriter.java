package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.setNecessaryJsonFactoryConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.impl.LazyValue;
import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil;

public class FullyFeaturedServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final Instance<ObjectMapper> originalMapper;
    private final Providers providers;
    private final LazyValue<ObjectWriter> defaultWriter;
    private final ConcurrentMap<String, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObjectWriter> perTypeWriter = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ObjectMapper> contextResolverMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<ObjectMapper, ObjectWriter> objectWriterMap = new ConcurrentHashMap<>();

    // used by Arc
    public FullyFeaturedServerJacksonMessageBodyWriter() {
        originalMapper = null;
        providers = null;
        defaultWriter = null;
    }

    @Inject
    public FullyFeaturedServerJacksonMessageBodyWriter(Instance<ObjectMapper> mapper, Providers providers) {
        this.originalMapper = mapper;
        this.providers = providers;
        this.defaultWriter = new LazyValue<>(new Supplier<>() {
            @Override
            public ObjectWriter get() {
                return createDefaultWriter(mapper.get());
            }
        });
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            ObjectMapper effectiveMapper = getEffectiveMapper(o, context);
            ObjectWriter effectiveWriter = getEffectiveWriter(effectiveMapper);
            ResteasyReactiveResourceInfo resourceInfo = context.getResteasyReactiveResourceInfo();
            if (resourceInfo != null) {
                ObjectWriter writerFromAnnotation = getObjectWriterFromAnnotations(resourceInfo, genericType, effectiveMapper);
                if (writerFromAnnotation != null) {
                    effectiveWriter = writerFromAnnotation;
                }

                Class<?> jsonViewValue = ResteasyReactiveServerJacksonRecorder.jsonViewForMethod(resourceInfo.getMethodId());
                if (jsonViewValue != null) {
                    effectiveWriter = effectiveWriter.withView(jsonViewValue);
                } else {
                    jsonViewValue = ResteasyReactiveServerJacksonRecorder
                            .jsonViewForClass(resourceInfo.getResourceClass());
                    if (jsonViewValue != null) {
                        effectiveWriter = effectiveWriter.withView(jsonViewValue);
                    }

                }
            }
            // make sure we properly handle polymorphism in generic collections
            if (genericType != null && o != null) {
                JavaType rootType = JacksonMapperUtil.getGenericRootType(genericType, effectiveWriter);
                // Check that the determined root type is really assignable from the given entity.
                // A mismatch can happen, if a ServerResponseFilter replaces the response entity with another object
                // that does not match the original signature of the method (see HalServerResponseFilter for an example)
                if (rootType != null && rootType.isTypeOrSuperTypeOf(o.getClass())) {
                    effectiveWriter = effectiveWriter.forType(rootType);
                }
            }
            effectiveWriter.writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    private ObjectWriter getObjectWriterFromAnnotations(ResteasyReactiveResourceInfo resourceInfo, Type type,
            ObjectMapper mapper) {
        // Check `@CustomSerialization` annotated in methods
        String methodId = resourceInfo.getMethodId();
        var customSerializationValue = ResteasyReactiveServerJacksonRecorder.customSerializationForMethod(methodId);
        if (customSerializationValue != null) {
            return perMethodWriter.computeIfAbsent(methodId,
                    new MethodObjectWriterFunction(customSerializationValue, type, mapper));
        }

        // Otherwise, check `@CustomSerialization` annotated in class. In this case, we use the effective type for caching up
        // the object.
        customSerializationValue = ResteasyReactiveServerJacksonRecorder
                .customSerializationForClass(resourceInfo.getResourceClass());
        if (customSerializationValue != null) {
            Type effectiveType = type;
            if (type instanceof ParameterizedType) {
                effectiveType = ((ParameterizedType) type).getActualTypeArguments()[0];
            }

            return perTypeWriter.computeIfAbsent(effectiveType.getTypeName(),
                    new MethodObjectWriterFunction(customSerializationValue, type, mapper));
        }

        return null;
    }

    private ObjectWriter getEffectiveWriter(ObjectMapper effectiveMapper) {
        if (effectiveMapper == originalMapper.get()) {
            return defaultWriter.get();
        }
        return objectWriterMap.computeIfAbsent(effectiveMapper, new Function<>() {
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
        ObjectMapper effectiveMapper = originalMapper.get();
        ContextResolver<ObjectMapper> contextResolver = providers.getContextResolver(ObjectMapper.class,
                context.getResponseMediaType());
        if (contextResolver == null) {
            // TODO: not sure if this is correct, but Jackson does this as well...
            contextResolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        if (contextResolver != null) {
            var cr = contextResolver;
            ObjectMapper mapperFromContextResolver = contextResolverMap.computeIfAbsent(o.getClass(), new Function<>() {
                @Override
                public ObjectMapper apply(Class<?> aClass) {
                    return cr.getContext(o.getClass());
                }
            });
            if (mapperFromContextResolver != null) {
                effectiveMapper = mapperFromContextResolver;
            }
        }
        return effectiveMapper;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, defaultWriter.get());
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
