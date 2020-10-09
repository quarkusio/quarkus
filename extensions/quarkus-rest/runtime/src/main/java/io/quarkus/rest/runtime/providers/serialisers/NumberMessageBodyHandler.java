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
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
public class NumberMessageBodyHandler extends PrimitiveBodyHandler implements MessageBodyReader<Number> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Number.class.isAssignableFrom(type);
    }

    @Override
    public Number readFrom(Class<Number> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String text = super.readFrom(entityStream, false);
        Class<? extends Number> damnit = type;
        // we initially had one provider per number type, but the TCK wants this Number provider to be overridable
        if (damnit == Byte.class || damnit == byte.class)
            return Byte.valueOf(text);
        if (damnit == Short.class || damnit == short.class)
            return Integer.valueOf(text);
        if (damnit == Integer.class || damnit == int.class)
            return Integer.valueOf(text);
        if (damnit == Long.class || damnit == long.class)
            return Long.valueOf(text);
        if (damnit == Float.class || damnit == float.class)
            return Float.valueOf(text);
        if (damnit == Double.class || damnit == double.class)
            return Double.valueOf(text);
        if (damnit == BigDecimal.class)
            return new BigDecimal(text);
        if (damnit == BigInteger.class)
            return new BigInteger(text);
        throw new RuntimeException("Don't know how to handle number class " + type);
    }

}
