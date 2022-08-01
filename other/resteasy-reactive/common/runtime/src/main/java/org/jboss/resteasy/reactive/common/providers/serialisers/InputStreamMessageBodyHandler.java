package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public class InputStreamMessageBodyHandler implements MessageBodyWriter<InputStream>, MessageBodyReader<InputStream> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public InputStream readFrom(Class<InputStream> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return entityStream;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(InputStream inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        writeTo(inputStream, entityStream);
    }

    protected void writeTo(InputStream inputStream, OutputStream entityStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int c;
            while ((c = inputStream.read(buffer)) != -1) {
                entityStream.write(buffer, 0, c);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Drop the exception so we don't mask real IO errors
            }
            try {
                entityStream.close();
            } catch (IOException e) {
                // Drop the exception so we don't mask real IO errors
            }
        }
    }
}
