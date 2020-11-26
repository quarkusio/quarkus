package io.quarkus.resteasy.reactive.jsonb.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.EmptyInputStream;
import org.jboss.resteasy.reactive.server.core.LazyMethod;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestMessageBodyReader;

public class JsonbMessageBodyReader implements QuarkusRestMessageBodyReader<Object> {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyReader(Jsonb json) {
        this.json = json;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) {
        if (entityStream instanceof EmptyInputStream) {
            return null;
        }
        Type runtimeType = genericType != null ? genericType : type;
        return json.fromJson(entityStream, runtimeType);
    }
}
