package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
public interface ServerMessageBodyWriter<T> extends MessageBodyWriter<T> {

    boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType);

    void writeResponse(T o, Type genericType, ServerRequestContext context) throws WebApplicationException, IOException;

    /**
     * A special super-class of MessageBodyWriters that accepts all types of input.
     * The main purpose of this class is to allow runtime code
     * to optimize for the case when there are multiple providers determined at build time
     * but the first one will always be used
     */
    abstract class AllWriteableMessageBodyWriter implements ServerMessageBodyWriter<Object> {

        @Override
        public final boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
                MediaType mediaType) {
            return true;
        }

        @Override
        public final boolean isWriteable(Class<?> type, Type genericType,
                Annotation[] annotations, MediaType mediaType) {
            return true;
        }
    }
}
