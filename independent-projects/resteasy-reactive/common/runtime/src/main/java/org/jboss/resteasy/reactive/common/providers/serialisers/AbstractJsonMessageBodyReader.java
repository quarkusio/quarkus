package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
        String subtype = mediaType.getSubtype();
        boolean isApplicationMediaType = "application".equals(mediaType.getType());
        return (isApplicationMediaType && "json".equalsIgnoreCase(subtype) || subtype.endsWith("+json")
                || subtype.equalsIgnoreCase("x-ndjson"))
                || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isApplicationMediaType));
    }
}
