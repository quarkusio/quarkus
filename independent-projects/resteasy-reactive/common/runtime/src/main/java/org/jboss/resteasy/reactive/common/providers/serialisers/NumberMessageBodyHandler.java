package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberMessageBodyHandler extends PrimitiveBodyHandler implements MessageBodyReader<Number> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Number.class.isAssignableFrom(type);
    }

    public Number readFrom(Class<Number> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return doReadFrom(type, entityStream);
    }

    protected Number doReadFrom(Class<Number> type, InputStream entityStream) throws IOException {
        String text = readFrom(entityStream, false);
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
