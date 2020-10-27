package io.quarkus.rest.server.test.resource.basic.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_PLAIN + "; charset=UTF-8")
public class MediaTypeFromMessageBodyWriterListAsText implements MessageBodyWriter<Iterable<?>> {

    @Override
    public long getSize(final Iterable<?> t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return -1L;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return Iterable.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(final Iterable<?> items, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream out) throws IOException {

        if (items instanceof Collection) {
            httpHeaders.putSingle("X-COUNT", Integer.toString(((Collection<?>) items).size()));
        }
    }
}
