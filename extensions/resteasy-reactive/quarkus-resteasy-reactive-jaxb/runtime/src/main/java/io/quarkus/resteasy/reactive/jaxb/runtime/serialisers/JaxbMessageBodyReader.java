package io.quarkus.resteasy.reactive.jaxb.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.server.runtime.StreamUtil;

public class JaxbMessageBodyReader implements ServerMessageBodyReader<Object> {

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
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
        boolean isApplicationMediaType = "application".equals(mediaType.getType());
        return (isApplicationMediaType && "xml".equalsIgnoreCase(subtype) || subtype.endsWith("+xml"))
                || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isApplicationMediaType));
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
