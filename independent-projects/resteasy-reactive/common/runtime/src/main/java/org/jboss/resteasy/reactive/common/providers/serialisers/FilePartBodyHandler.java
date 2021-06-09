package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.FilePart;

public class FilePartBodyHandler implements MessageBodyWriter<FilePart> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return FilePart.class.isAssignableFrom(type);
    }

    public void writeTo(FilePart uploadFile, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(uploadFile.count));
        doWrite(uploadFile, entityStream);
    }

    protected void doWrite(FilePart uploadFile, OutputStream out) throws IOException {
        PathPartBodyHandler.doWrite(new BufferedInputStream(new FileInputStream(uploadFile.file)), uploadFile.offset,
                uploadFile.count, out);
    }
}
