package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.setNecessaryJsonFactoryConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;

public class FullyFeaturedServerJacksonMessageBodyReader extends AbstractServerJacksonMessageBodyReader
        implements ServerMessageBodyReader<Object> {

    private final Instance<ObjectMapper> originalMapper;
    private final Providers providers;
    private final ConcurrentMap<String, ObjectReader> perMethodReader = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObjectReader> perTypeReader = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ObjectMapper> contextResolverMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<ObjectMapper, ObjectReader> objectReaderMap = new ConcurrentHashMap<>();

    // used by Arc
    public FullyFeaturedServerJacksonMessageBodyReader() {
        originalMapper = null;
        providers = null;
    }

    @Inject
    public FullyFeaturedServerJacksonMessageBodyReader(Instance<ObjectMapper> mapper, Providers providers) {
        super(mapper);
        this.originalMapper = mapper;
        this.providers = providers;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return doReadFrom(type, genericType, mediaType, entityStream);
        } catch (MismatchedInputException | InvalidDefinitionException e) {
            /*
             * To extract additional details when running in dev mode or test mode, Quarkus previously offered the
             * DefaultMismatchedInputException(Mapper). That mapper provides additional details about bad input,
             * beyond Jackson's default, when running in Dev or Test mode. To preserve that behavior, we rethrow
             * MismatchedInputExceptions we encounter.
             *
             * An InvalidDefinitionException is thrown when there is a problem with the way a type is
             * set up/annotated for consumption by the Jackson API. We don't wrap it in a WebApplicationException
             * (as a Server Error), since unhandled exceptions will end up as a 500 anyway. In addition, this
             * allows built-in features like the NativeInvalidDefinitionExceptionMapper to be registered and
             * communicate potential Jackson integration issues, and potential solutions for resolving them.
             */
            throw e;
        } catch (StreamReadException | DatabindException e) {
            /*
             * As JSON is evaluated, it can be invalid due to one of two reasons:
             * 1) Malformed JSON. Un-parsable JSON results in a StreamReadException
             * 2) Valid JSON that violates some binding constraint, i.e., a required property, mismatched data types, etc.
             * Violations of these types are captured via a DatabindException.
             */
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return readFrom(type, genericType, null, mediaType, null, context.getInputStream());
    }

    private Object doReadFrom(Class<Object> type, Type genericType, MediaType responseMediaType, InputStream entityStream)
            throws IOException {
        if (StreamUtil.isEmpty(entityStream)) {
            return null;
        }
        try {
            ObjectReader reader = getEffectiveReader(type, genericType, responseMediaType);
            return reader.forType(reader.getTypeFactory().constructType(genericType != null ? genericType : type))
                    .readValue(entityStream);
        } catch (MismatchedInputException e) {
            if (isEmptyInputException(e)) {
                return null;
            }
            throw e;
        }
    }

    private boolean isEmptyInputException(MismatchedInputException e) {
        // this isn't great, but Jackson doesn't have a specific exception for empty input...
        return e.getMessage().startsWith("No content");
    }

    private ObjectReader getObjectReaderFromAnnotations(ResteasyReactiveResourceInfo resourceInfo, Type type,
            ObjectMapper mapper) {
        // Check `@CustomDeserialization` annotated in methods
        String methodId = resourceInfo.getMethodId();
        var customDeserializationValue = ResteasyReactiveServerJacksonRecorder.customDeserializationForMethod(methodId);
        if (customDeserializationValue != null) {
            return perMethodReader.computeIfAbsent(methodId,
                    new FullyFeaturedServerJacksonMessageBodyReader.MethodObjectReaderFunction(customDeserializationValue, type,
                            mapper));
        }

        // Otherwise, check `@CustomDeserialization` annotated in class. In this case, we use the effective type for caching up
        // the object.
        customDeserializationValue = ResteasyReactiveServerJacksonRecorder
                .customDeserializationForClass(resourceInfo.getResourceClass());
        if (customDeserializationValue != null) {
            Type effectiveType = type;
            if (type instanceof ParameterizedType) {
                effectiveType = ((ParameterizedType) type).getActualTypeArguments()[0];
            }

            return perTypeReader.computeIfAbsent(effectiveType.getTypeName(),
                    new FullyFeaturedServerJacksonMessageBodyReader.MethodObjectReaderFunction(customDeserializationValue, type,
                            mapper));
        }

        return null;
    }

    private ObjectReader getEffectiveReader(Class<Object> type, Type genericType, MediaType responseMediaType) {
        ObjectMapper effectiveMapper = getEffectiveMapper(type, responseMediaType);
        ObjectReader effectiveReader = defaultReader.get();
        if (effectiveMapper != originalMapper) {
            // Effective reader based on the context
            effectiveReader = objectReaderMap.computeIfAbsent(effectiveMapper, new Function<>() {
                @Override
                public ObjectReader apply(ObjectMapper objectMapper) {
                    return objectMapper.reader();
                }
            });
        }

        // Get object reader from context if configured
        ServerRequestContext context = CurrentRequestManager.get();
        if (context != null) {
            ResteasyReactiveResourceInfo resourceInfo = context.getResteasyReactiveResourceInfo();
            if (resourceInfo != null) {
                ObjectReader readerFromAnnotation = getObjectReaderFromAnnotations(resourceInfo, genericType, effectiveMapper);
                if (readerFromAnnotation != null) {
                    effectiveReader = readerFromAnnotation;
                }

                Class<?> jsonViewValue = ResteasyReactiveServerJacksonRecorder
                        .jsonViewForMethod("request-body;" + resourceInfo.getMethodId());
                if (jsonViewValue != null) {
                    return effectiveReader.withView(jsonViewValue);
                }
            }
        }

        return effectiveReader;
    }

    private ObjectMapper getEffectiveMapper(Class<Object> type, MediaType responseMediaType) {
        if (providers == null) {
            return originalMapper.get();
        }

        ContextResolver<ObjectMapper> contextResolver = providers.getContextResolver(ObjectMapper.class,
                responseMediaType);
        if (contextResolver == null) {
            // TODO: not sure if this is correct, but Jackson does this as well...
            contextResolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        if (contextResolver != null) {
            var cr = contextResolver;
            ObjectMapper result = contextResolverMap.computeIfAbsent(type, new Function<>() {
                @Override
                public ObjectMapper apply(Class<?> aClass) {
                    return cr.getContext(type);
                }
            });
            if (result != null) {
                return result;
            }
        }

        return originalMapper.get();
    }

    private static class MethodObjectReaderFunction implements Function<String, ObjectReader> {
        private final Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>> clazz;
        private final Type genericType;
        private final ObjectMapper originalMapper;

        public MethodObjectReaderFunction(Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>> clazz, Type genericType,
                ObjectMapper originalMapper) {
            this.clazz = clazz;
            this.genericType = genericType;
            this.originalMapper = originalMapper;
        }

        @Override
        public ObjectReader apply(String methodId) {
            try {
                BiFunction<ObjectMapper, Type, ObjectReader> biFunctionInstance = clazz.getDeclaredConstructor().newInstance();
                ObjectReader objectReader = biFunctionInstance.apply(originalMapper, genericType);
                setNecessaryJsonFactoryConfig(objectReader.getFactory());
                return objectReader;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
