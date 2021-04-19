package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.FilePart;
import org.jboss.resteasy.reactive.common.providers.serialisers.FilePartBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

// TODO: this is very simplistic at the moment

@Provider
@Produces("*/*")
@Consumes("*/*")
public class ServerFilePartBodyHandler extends FilePartBodyHandler implements ServerMessageBodyWriter<FilePart> {

    @Override
    public long getSize(FilePart o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.file.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return FilePart.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(FilePart o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        ServerHttpResponse vertxResponse = context.serverResponse();
        vertxResponse.sendFile(o.file.getPath(), o.offset, o.count);
    }
}
