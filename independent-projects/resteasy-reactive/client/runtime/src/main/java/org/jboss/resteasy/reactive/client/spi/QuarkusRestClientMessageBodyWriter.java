package org.jboss.resteasy.reactive.client.spi;

import io.vertx.core.buffer.Buffer;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x buffer
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QuarkusRestClientMessageBodyWriter<T> extends MessageBodyWriter<T> {

    public Buffer writeResponse(T o) throws WebApplicationException;

}
