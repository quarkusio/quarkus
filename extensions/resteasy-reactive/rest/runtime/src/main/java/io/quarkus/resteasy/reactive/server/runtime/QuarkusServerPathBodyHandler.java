package io.quarkus.resteasy.reactive.server.runtime;

import static org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler.PREFIX;
import static org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler.SUFFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class QuarkusServerPathBodyHandler implements ServerMessageBodyReader<Path> {

    private static final Logger log = Logger.getLogger(QuarkusServerPathBodyHandler.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
            MediaType mediaType) {
        return Path.class.equals(type);
    }

    @Override
    public Path readFrom(Class<Path> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        Path file = createFile(context);
        return FileBodyHandler.doRead(context.getRequestHeaders().getRequestHeaders(), context.getInputStream(), file.toFile())
                .toPath();
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class.equals(type);
    }

    @Override
    public Path readFrom(Class<Path> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        // unfortunately we don't do much here to avoid the file leak
        // however this should never be called in a real world scenario
        return FileBodyHandler.doRead(httpHeaders, entityStream, Files.createTempFile(PREFIX, SUFFIX).toFile()).toPath();
    }

    static Path createFile(ServerRequestContext context) throws IOException {
        RuntimeConfiguration.Body runtimeBodyConfiguration = ResteasyReactiveRecorder.getCurrentDeployment()
                .getRuntimeConfiguration().body();
        boolean deleteUploadedFilesOnEnd = runtimeBodyConfiguration.deleteUploadedFilesOnEnd();
        String uploadsDirectoryStr = runtimeBodyConfiguration.uploadsDirectory();
        Path uploadDirectory = Paths.get(uploadsDirectoryStr);
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Path file = Files.createTempFile(uploadDirectory, PREFIX, SUFFIX);
        if (deleteUploadedFilesOnEnd) {
            context.registerCompletionCallback(new CompletionCallback() {
                @Override
                public void onComplete(Throwable throwable) {
                    ResteasyReactiveRecorder.EXECUTOR_SUPPLIER.get().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (Files.exists(file)) {
                                try {
                                    Files.delete(file);
                                } catch (NoSuchFileException e) { // ignore
                                } catch (IOException e) {
                                    log.error("Cannot remove uploaded file " + file, e);
                                }
                            }
                        }
                    });
                }
            });
        }
        return file;
    }
}
