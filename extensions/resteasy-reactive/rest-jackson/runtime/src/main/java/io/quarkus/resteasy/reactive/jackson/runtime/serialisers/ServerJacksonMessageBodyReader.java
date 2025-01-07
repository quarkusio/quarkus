package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class ServerJacksonMessageBodyReader extends AbstractServerJacksonMessageBodyReader
        implements ServerMessageBodyReader<Object> {

    // used by Arc
    public ServerJacksonMessageBodyReader() {

    }

    @Inject
    public ServerJacksonMessageBodyReader(Instance<ObjectMapper> mapper) {
        super(mapper);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return doReadFrom(type, genericType, entityStream);
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

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        if (StreamUtil.isEmpty(entityStream)) {
            return null;
        }
        try {
            ObjectReader reader = getEffectiveReader();
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
}
