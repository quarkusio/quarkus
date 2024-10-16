package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;

public abstract class AbstractJsonMessageBodyReader implements MessageBodyReader<Object> {

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
        String subtype = mediaType.getSubtype().toLowerCase();
        final String mainType = mediaType.getType().toLowerCase();
        boolean isApplicationMediaType = "application".equals(mainType);
        return (isApplicationMediaType && "json".equals(subtype) || subtype.endsWith("+json")
                || "x-ndjson".equals(subtype))
                || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isApplicationMediaType));
    }
}
