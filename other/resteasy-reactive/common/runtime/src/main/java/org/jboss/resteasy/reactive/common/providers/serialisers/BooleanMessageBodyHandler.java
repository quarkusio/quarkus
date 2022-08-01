package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

public class BooleanMessageBodyHandler extends PrimitiveBodyHandler implements MessageBodyReader<Boolean> {
    public Boolean readFrom(Class<Boolean> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return Boolean.valueOf(readFrom(entityStream, false));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Boolean.class;
    }
}
