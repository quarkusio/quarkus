package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class FullyFeaturedServerJacksonMessageBodyReader extends JacksonBasicMessageBodyReader
        implements ServerMessageBodyReader<Object> {

    private final Providers providers;
    private final ConcurrentMap<ObjectMapper, ObjectReader> contextResolverMap = new ConcurrentHashMap<>();

    @Inject
    public FullyFeaturedServerJacksonMessageBodyReader(ObjectMapper mapper, Providers providers) {
        super(mapper);
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
            ObjectReader reader = getEffectiveReader(type, responseMediaType);
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

    private ObjectReader getEffectiveReader(Class<Object> type, MediaType responseMediaType) {
        ObjectMapper effectiveMapper = getObjectMapperFromContext(type, responseMediaType);
        if (effectiveMapper == null) {
            return getEffectiveReader();
        }

        return contextResolverMap.computeIfAbsent(effectiveMapper, new Function<>() {
            @Override
            public ObjectReader apply(ObjectMapper objectMapper) {
                return objectMapper.reader();
            }
        });
    }

    private ObjectMapper getObjectMapperFromContext(Class<Object> type, MediaType responseMediaType) {
        if (providers == null) {
            return null;
        }

        ContextResolver<ObjectMapper> contextResolver = providers.getContextResolver(ObjectMapper.class,
                responseMediaType);
        if (contextResolver == null) {
            // TODO: not sure if this is correct, but Jackson does this as well...
            contextResolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        if (contextResolver != null) {
            return contextResolver.getContext(type);
        }

        return null;
    }
}
