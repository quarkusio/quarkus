package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

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
