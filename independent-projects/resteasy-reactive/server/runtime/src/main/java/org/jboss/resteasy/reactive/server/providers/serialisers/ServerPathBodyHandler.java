package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.providers.serialisers.PathBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Produces("*/*")
public class ServerPathBodyHandler extends PathBodyHandler implements ServerMessageBodyWriter<java.nio.file.Path> {

    @Override
    public long getSize(java.nio.file.Path o, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        try {
            return Files.size(o);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
            MediaType mediaType) {
        return java.nio.file.Path.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(java.nio.file.Path o, Type genericType, ServerRequestContext context)
            throws WebApplicationException {
        ServerFileBodyHandler.sendFile(o.toFile(), context);
    }
}
