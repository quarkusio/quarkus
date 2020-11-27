package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?                                ``
public interface ServerMessageBodyWriter<T> extends MessageBodyWriter<T> {

    boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType);

    void writeResponse(T o, ServerRequestContext context) throws WebApplicationException, IOException;

}
