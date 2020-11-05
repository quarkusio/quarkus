package org.jboss.resteasy.reactive.common.runtime.providers.serialisers;

import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

public class VertxBufferMessageBodyWriter implements MessageBodyWriter<Buffer> {
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public void writeTo(Buffer buffer, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(buffer.getBytes());
    }
}
