package io.quarkus.resteasy.reactive.server.runtime;

import static io.quarkus.resteasy.reactive.server.runtime.QuarkusServerPathBodyHandler.createFile;
import static org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler.PREFIX;
import static org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler.SUFFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class QuarkusServerFileBodyHandler implements ServerMessageBodyReader<File> {

    private static final Logger log = Logger.getLogger(QuarkusServerFileBodyHandler.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
            MediaType mediaType) {
        return File.class.equals(type);
    }

    @Override
    public File readFrom(Class<File> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        Path file = createFile(context);
        return FileBodyHandler.doRead(context.getRequestHeaders().getRequestHeaders(), context.getInputStream(), file.toFile());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class.equals(type);
    }

    @Override
    public File readFrom(Class<File> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        // unfortunately we don't do much here to avoid the file leak
        // however this should never be called in a real world scenario
        return FileBodyHandler.doRead(httpHeaders, entityStream, Files.createTempFile(PREFIX, SUFFIX).toFile());
    }
}
