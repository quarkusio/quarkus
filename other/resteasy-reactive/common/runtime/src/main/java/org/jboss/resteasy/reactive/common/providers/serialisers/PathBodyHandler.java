package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

public class PathBodyHandler implements MessageBodyWriter<java.nio.file.Path> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return java.nio.file.Path.class.isAssignableFrom(type);
    }

    public void writeTo(java.nio.file.Path uploadFile, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(uploadFile)));
        doWrite(uploadFile, entityStream);
    }

    protected void doWrite(java.nio.file.Path uploadFile, OutputStream out) throws IOException {
        Files.copy(uploadFile, out);
    }
}
