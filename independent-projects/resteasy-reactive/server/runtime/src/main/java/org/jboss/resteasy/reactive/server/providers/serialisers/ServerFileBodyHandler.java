package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
@Produces("*/*")
@Consumes("*/*")
public class ServerFileBodyHandler extends FileBodyHandler implements ServerMessageBodyWriter<File> {

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        context.serverResponse().sendFile(o.getAbsolutePath(), 0, o.length());
    }
}
