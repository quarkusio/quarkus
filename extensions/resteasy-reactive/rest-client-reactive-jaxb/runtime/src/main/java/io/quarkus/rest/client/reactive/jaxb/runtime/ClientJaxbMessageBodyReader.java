package io.quarkus.rest.client.reactive.jaxb.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.reactive.common.util.StreamUtil;

public class ClientJaxbMessageBodyReader implements MessageBodyReader<Object> {

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    protected boolean isReadable(MediaType mediaType, Class<?> type) {
        if (mediaType == null) {
            return false;
        }
        if (String.class.equals(type)) { // don't attempt to read plain strings
            return false;
        }
        String subtype = mediaType.getSubtype();
        boolean isCorrectMediaType = "application".equals(mediaType.getType()) || "text".equals(mediaType.getType());
        return (isCorrectMediaType && "xml".equalsIgnoreCase(subtype) || subtype.endsWith("+xml"))
                || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isCorrectMediaType));
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        if (isInputStreamEmpty(entityStream)) {
            return null;
        }

        return JAXB.unmarshal(entityStream, type);
    }

    private boolean isInputStreamEmpty(InputStream entityStream) throws IOException {
        return StreamUtil.isEmpty(entityStream) || entityStream.available() == 0;
    }
}
