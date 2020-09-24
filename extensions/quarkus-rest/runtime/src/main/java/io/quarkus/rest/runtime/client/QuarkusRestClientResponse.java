package io.quarkus.rest.runtime.client;

import java.io.IOException;
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

    Serialisers serialisers;

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return readEntity(entityType, null, null);
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
        if (hasEntity() && entityType.equals(String.class)) {
            return (T) getEntity().toString();
        }

        MediaType mediaType = getMediaType();
        List<MessageBodyReader<?>> readers = serialisers.findReaders(entityType, mediaType, RuntimeType.CLIENT);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(entityType, genericType, annotations, mediaType)) {
                Object entity;
                try {
                    entity = ((MessageBodyReader) reader).readFrom(entityType, genericType,
                            annotations, mediaType, getStringHeaders(), getEntityStream());
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
