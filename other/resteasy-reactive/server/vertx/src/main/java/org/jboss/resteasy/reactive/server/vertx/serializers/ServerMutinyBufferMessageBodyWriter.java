package org.jboss.resteasy.reactive.server.vertx.serializers;

import io.vertx.mutiny.core.buffer.Buffer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
public class ServerMutinyBufferMessageBodyWriter implements ServerMessageBodyWriter<Buffer> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public void writeTo(Buffer buffer, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(buffer.getBytes());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, Type genericType, ServerRequestContext context) throws WebApplicationException {
        context.serverResponse().end(buffer.getBytes());
    }
}
