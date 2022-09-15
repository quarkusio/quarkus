package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ReaderBodyHandler implements MessageBodyWriter<Reader>, MessageBodyReader<Reader> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(Reader.class);
    }

    public Reader readFrom(Class<Reader> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return new InputStreamReader(entityStream, MessageReaderUtil.charsetFromMediaType(mediaType));
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Reader.class.isAssignableFrom(type);
    }

    public void writeTo(Reader inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        try {
            int c;
            while ((c = inputStream.read()) != -1) {
                entityStream.write(c);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Drop the exception so we don't mask real IO errors
            }
        }
    }
}
