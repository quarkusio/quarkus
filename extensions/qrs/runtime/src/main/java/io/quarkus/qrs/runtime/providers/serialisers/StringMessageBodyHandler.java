package io.quarkus.qrs.runtime.providers.serialisers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.quarkus.qrs.runtime.core.LazyMethod;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;
import io.vertx.core.http.HttpServerResponse;

@Provider
public class StringMessageBodyHandler implements QrsMessageBodyWriter<Object>, MessageBodyReader<String> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        // FIXME: use response encoding
        entityStream.write(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, QrsRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        HttpServerResponse vertxResponse = context.getContext().response();
        vertxResponse.end(o.toString());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(String.class);
    }

    @Override
    public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024]; //TODO: fix, needs a pure vert.x async read model
        int r;
        while ((r = entityStream.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
