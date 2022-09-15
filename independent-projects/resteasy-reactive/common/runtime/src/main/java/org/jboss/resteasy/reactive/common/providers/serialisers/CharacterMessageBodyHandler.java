package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class CharacterMessageBodyHandler extends PrimitiveBodyHandler implements MessageBodyReader<Character> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Character.class;
    }

    public Character readFrom(Class<Character> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return doReadFrom(entityStream);
    }

    protected char doReadFrom(InputStream entityStream) throws IOException {
        String string = readFrom(entityStream, false);
        if (string.length() == 1)
            return string.charAt(0);
        throw new BadRequestException("Invalid character: " + string);
    }
}
