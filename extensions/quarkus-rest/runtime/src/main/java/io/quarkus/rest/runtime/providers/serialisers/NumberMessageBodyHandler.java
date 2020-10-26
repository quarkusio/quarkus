package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyReader;

@Provider
public class NumberMessageBodyHandler extends PrimitiveBodyHandler implements QuarkusRestMessageBodyReader<Number> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Number.class.isAssignableFrom(type);
    }

    @Override
    public Number readFrom(Class<Number> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return doReadFrom(type, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return Number.class.isAssignableFrom(type);
    }

    @Override
    public Number readFrom(Class<Number> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, context.getInputStream());
    }

    private Number doReadFrom(Class<Number> type, InputStream entityStream) throws IOException {
        String text = super.readFrom(entityStream, false);
        // we initially had one provider per number type, but the TCK wants this Number provider to be overridable
        if ((Class<? extends Number>) type == Byte.class || (Class<? extends Number>) type == byte.class)
            return Byte.valueOf(text);
        if ((Class<? extends Number>) type == Short.class || (Class<? extends Number>) type == short.class)
            return Integer.valueOf(text);
        if ((Class<? extends Number>) type == Integer.class || (Class<? extends Number>) type == int.class)
            return Integer.valueOf(text);
        if ((Class<? extends Number>) type == Long.class || (Class<? extends Number>) type == long.class)
            return Long.valueOf(text);
        if ((Class<? extends Number>) type == Float.class || (Class<? extends Number>) type == float.class)
            return Float.valueOf(text);
        if ((Class<? extends Number>) type == Double.class || (Class<? extends Number>) type == double.class)
            return Double.valueOf(text);
        if ((Class<? extends Number>) type == BigDecimal.class)
            return new BigDecimal(text);
        if ((Class<? extends Number>) type == BigInteger.class)
            return new BigInteger(text);
        throw new RuntimeException("Don't know how to handle number class " + type);
    }
}
