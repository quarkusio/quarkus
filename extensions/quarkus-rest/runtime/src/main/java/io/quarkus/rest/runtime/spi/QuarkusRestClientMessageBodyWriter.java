package io.quarkus.rest.runtime.spi;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyWriter;

import io.vertx.core.buffer.Buffer;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x buffer
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QuarkusRestClientMessageBodyWriter<T> extends MessageBodyWriter<T> {

    public Buffer writeResponse(T o) throws WebApplicationException;

}
