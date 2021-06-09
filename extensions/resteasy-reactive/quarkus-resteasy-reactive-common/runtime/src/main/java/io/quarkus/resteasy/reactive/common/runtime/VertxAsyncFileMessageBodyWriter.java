package io.quarkus.resteasy.reactive.common.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import io.vertx.core.file.AsyncFile;

public class VertxAsyncFileMessageBodyWriter implements MessageBodyWriter<AsyncFile> {
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // allow for subtypes, such as AsyncFileImpl
        return AsyncFile.class.isAssignableFrom(type);
    }

    public void writeTo(AsyncFile asyncFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        throw new UnsupportedOperationException("Returning an AsyncFile is not supported with WriterInterceptors");
    }
}
