package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

@Provider
public class VertxBufferMessageBodyWriter implements QuarkusRestMessageBodyWriter<Buffer> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Buffer buffer, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(buffer.getBytes());
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, QuarkusRestRequestContext context) throws WebApplicationException {
        HttpServerResponse vertxResponse = context.getContext().response();
        vertxResponse.end(buffer);
    }
}
