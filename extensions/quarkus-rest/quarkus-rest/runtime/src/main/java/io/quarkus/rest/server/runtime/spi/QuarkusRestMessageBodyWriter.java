package io.quarkus.rest.server.runtime.spi;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QuarkusRestMessageBodyWriter<T> extends MessageBodyWriter<T> {

    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType);

    public void writeResponse(T o, QuarkusRestRequestContext context) throws WebApplicationException, IOException;

}
