package io.quarkus.qrs.runtime.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.RequestContext;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QrsMessageBodyWriter<T> extends MessageBodyWriter<T> {

    public void writeTo(T o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, RequestContext context) throws WebApplicationException;

}
