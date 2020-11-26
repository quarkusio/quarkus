package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QuarkusRestMessageBodyWriter<T> extends MessageBodyWriter<T> {

    boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType);

    void writeResponse(T o, ResteasyReactiveRequestContext context) throws WebApplicationException, IOException;

}
