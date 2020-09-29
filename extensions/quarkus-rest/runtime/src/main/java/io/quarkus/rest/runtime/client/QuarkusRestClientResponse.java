package io.quarkus.rest.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;

/**
 * This is the Response class client response
 * object with more deserialising powers than user-created responses @{link {@link QuarkusRestResponse}.
 */
public class QuarkusRestClientResponse extends QuarkusRestResponse {

    InvocationState invocationState;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T> T readEntity(Class<T> entityType, Type genericType, Annotation[] annotations) {
        // TODO: we probably need better state handling
        if (hasEntity() && entityType.isInstance(getEntity())) {
            // Note that this works if entityType is InputStream where we return it without closing it, as per spec
            return (T) getEntity();
        }
        // FIXME: does the spec really tell us to do this? sounds like a workaround for not having a string reader
        if (hasEntity() && entityType.equals(String.class)) {
            return (T) getEntity().toString();
        }

        checkClosed();

        // apparently we're trying to re-read it here, even if we already have an entity, as long as it's not the right
        // type
        // Note that this will get us the entity if it's an InputStream because setEntity checks that
        InputStream entityStream = getEntityStream();
        if (entityStream == null)
            return null;

        // Spec says to return the input stream as-is, without closing it, if that's what we want
        if (InputStream.class.isAssignableFrom(entityType)) {
            return (T) entityStream;
        }
        MediaType mediaType = getMediaType();
        List<MessageBodyReader<?>> readers = invocationState.serialisers.findReaders(invocationState.configuration, entityType,
                mediaType, RuntimeType.CLIENT);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(entityType, genericType, annotations, mediaType)) {
                Object entity;
                try {
                    // it's possible we already read it for a different type, so try to reset it
                    if (buffered) {
                        entityStream.reset();
                    } else if (consumed) {
                        throw new IllegalStateException(
                                "Entity stream has already been read and is not buffered: call Reponse.bufferEntity()");
                    }
                    entity = Serialisers.invokeClientReader(annotations, entityType, genericType, mediaType,
                            invocationState.properties, getStringHeaders(), reader,
                            entityStream, invocationState.getReaderInterceptors());
                    consumed = true;
                    // spec says to close ourselves
                    close();
                } catch (IOException e) {
                    throw new ProcessingException(e);
                }
                setEntity(entity);
                return (T) entity;
            }
        }
        setEntity(null);
        // Spec says to throw this
        throw new ProcessingException(
                "Request could not be mapped to type " + (genericType != null ? genericType : entityType));
    }
}
