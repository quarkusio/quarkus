package org.jboss.resteasy.reactive.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.jaxrs.RestResponseImpl;
import org.jboss.resteasy.reactive.common.util.EmptyInputStream;

/**
 * This is the RestResponse class client response
 * object with more deserialising powers than user-created responses @{link {@link RestResponseImpl}.
 */
public class ClientRestResponseImpl<T> extends RestResponseImpl<T> {

    RestClientRequestContext restClientRequestContext;

    @SuppressWarnings({ "unchecked" })
    protected <OtherT> OtherT readEntity(Class<OtherT> entityType, Type genericType, Annotation[] annotations) {
        // TODO: we probably need better state handling
        if (entity != null && entityType.isInstance(entity)) {
            // Note that this works if entityType is InputStream where we return it without closing it, as per spec
            return (OtherT) entity;
        }

        checkClosed();

        // apparently we're trying to re-read it here, even if we already have an entity, as long as it's not the right
        // type
        // Note that this will get us the entity if it's an InputStream because setEntity checks that
        InputStream entityStream = getEntityStream();
        if (entityStream == null) {
            entityStream = new EmptyInputStream();
        }

        // it's possible we already read it for a different type, so try to reset it
        try {
            if (buffered) {
                entityStream.reset();
            } else if (consumed) {
                throw new IllegalStateException(
                        "Entity stream has already been read and is not buffered: call Reponse.bufferEntity()");
            }
        } catch (IOException e) {
            throw new ProcessingException(e);
        }

        // Spec says to return the input stream as-is, without closing it, if that's what we want
        if (InputStream.class.isAssignableFrom(entityType)) {
            return (OtherT) entityStream;
        }

        MediaType mediaType = getMediaType();
        try {
            entity = (T) ClientSerialisers.invokeClientReader(annotations, entityType, genericType, mediaType,
                    restClientRequestContext.properties, getStringHeaders(),
                    restClientRequestContext.getRestClient().getClientContext().getSerialisers(),
                    entityStream, restClientRequestContext.getReaderInterceptors(), restClientRequestContext.configuration);
            consumed = true;
            close();
            return (OtherT) entity;
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }
}
