package io.quarkus.rest.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;

public class QuarkusRestClientResponse extends QuarkusRestResponse {

    InvocationState invocationState;

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return readEntity(entityType, entityType, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), annotations);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T readEntity(Class<T> entityType, Type genericType, Annotation[] annotations) {
        // TODO: we probably need better state handling
        if (hasEntity() && entityType.isInstance(getEntity())) {
            return (T) getEntity();
        }
        // FIXME: does the spec really tell us to do this? sounds like a workaround for not having a string reader
        if (hasEntity() && entityType.equals(String.class)) {
            return (T) getEntity().toString();
        }

        // apparently we're trying to re-read it here, even if we already have an entity, as long as it's not the right
        // type
        MediaType mediaType = getMediaType();
        List<MessageBodyReader<?>> readers = invocationState.serialisers.findReaders(entityType, mediaType, RuntimeType.CLIENT);
        InputStream entityStream = getEntityStream();
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(entityType, genericType, annotations, mediaType)) {
                Object entity;
                try {
                    // it's possible we already read it for a different type, so try to reset it
                    if (entityStream.markSupported()) {
                        entityStream.reset();
                    }
                    entity = Serialisers.invokeClientReader(annotations, entityType, genericType, mediaType,
                            invocationState.properties, getStringHeaders(), reader,
                            entityStream, invocationState.getReaderInterceptors());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                setEntity(entity);
                return (T) entity;
            }
        }
        setEntity(null);
        return null;
    }
}
