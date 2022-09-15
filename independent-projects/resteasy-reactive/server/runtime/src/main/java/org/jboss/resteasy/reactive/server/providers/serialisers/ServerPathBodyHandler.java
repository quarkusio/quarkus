package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import org.jboss.resteasy.reactive.common.providers.serialisers.PathBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
@Produces("*/*")
public class ServerPathBodyHandler extends PathBodyHandler implements ServerMessageBodyWriter<java.nio.file.Path> {

    @Override
    public long getSize(java.nio.file.Path o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        try {
            return Files.size(o);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return java.nio.file.Path.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(java.nio.file.Path o, Type genericType, ServerRequestContext context)
            throws WebApplicationException {
        ServerHttpResponse serverResponse = context.serverResponse();
        // sendFile implies end(), even though javadoc doesn't say, if you add end() it will throw
        serverResponse.sendFile(o.toString(), 0, Long.MAX_VALUE);
    }
}
