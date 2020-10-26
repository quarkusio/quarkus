package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyReader;

@Provider
public class BooleanMessageBodyHandler extends PrimitiveBodyHandler implements QuarkusRestMessageBodyReader<Boolean> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Boolean.class;
    }

    @Override
    public Boolean readFrom(Class<Boolean> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return Boolean.valueOf(super.readFrom(entityStream, false));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type == Boolean.class;
    }

    @Override
    public Boolean readFrom(Class<Boolean> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return Boolean.valueOf(super.readFrom(context.getInputStream(), false));
    }
}
