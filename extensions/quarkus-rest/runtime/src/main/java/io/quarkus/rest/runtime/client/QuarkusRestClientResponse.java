package io.quarkus.rest.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T readEntity(Class<T> entityType, Type genericType, Annotation[] annotations) {
        if (entityType.isInstance(getEntity())) {
            return (T) getEntity();
        }

        if (!(getEntityStream() instanceof ByteArrayInputStream)) {
            throw new IllegalStateException("Data cannot be re-read");
        }

        MediaType mediaType = getMediaType();
        List<MessageBodyReader<?>> readers = serialisers.findReaders(entityType, mediaType);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(entityType, genericType, annotations, mediaType)) {
                Object entity;
                try {
                    entity = ((MessageBodyReader) reader).readFrom(entityType, genericType,
                            annotations, mediaType, getStringHeaders(), getEntityStream());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return (T) entity;
            }
        }

        return null;
    }
}
