package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.PathPart;
import org.jboss.resteasy.reactive.common.providers.serialisers.PathPartBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
@Produces("*/*")
public class ServerPathPartBodyHandler extends PathPartBodyHandler implements ServerMessageBodyWriter<PathPart> {

    @Override
    public long getSize(PathPart o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        try {
            return Files.size(o.file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return PathPart.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(PathPart o, Type genericType, ServerRequestContext context)
            throws WebApplicationException {
        ServerHttpResponse serverResponse = context.serverResponse();
        // sendFile implies end(), even though javadoc doesn't say, if you add end() it will throw
        serverResponse.sendFile(o.file.toString(), o.offset, o.count);
    }
}
