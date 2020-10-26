package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyReader;

public class ReaderBodyHandler implements MessageBodyWriter<Reader>, QuarkusRestMessageBodyReader<Reader> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(Reader.class);
    }

    public Reader readFrom(Class<Reader> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return new InputStreamReader(entityStream, MessageReaderUtil.charsetFromMediaType(mediaType));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(Reader.class);
    }

    @Override
    public Reader readFrom(Class<Reader> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return new InputStreamReader(context.getInputStream(), MessageReaderUtil.charsetFromMediaType(mediaType));
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Reader.class.isAssignableFrom(type);
    }

    public long getSize(Reader inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
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
