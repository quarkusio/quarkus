package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import org.jboss.resteasy.reactive.PathPart;

public class PathPartBodyHandler implements MessageBodyWriter<PathPart> {

    public static final int BUFFER_SIZE = 8192;

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return PathPart.class.isAssignableFrom(type);
    }

    public void writeTo(PathPart uploadFile, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(uploadFile.count));
        doWrite(uploadFile, entityStream);
    }

    protected void doWrite(PathPart uploadFile, OutputStream out) throws IOException {
        doWrite(Files.newInputStream(uploadFile.file), uploadFile.offset, uploadFile.count, out);
    }

    static void doWrite(InputStream inputStream, long offset, long count, OutputStream out) throws IOException {
        try (InputStream in = inputStream) {
            in.skip(offset);
            long remaining = count;
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buf, 0, Math.min(BUFFER_SIZE, (int) remaining))) > 0) {
                out.write(buf, 0, n);
                remaining -= n;
            }
        }
    }
}
