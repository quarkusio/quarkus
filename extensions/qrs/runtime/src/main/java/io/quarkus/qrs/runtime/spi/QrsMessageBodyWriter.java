package io.quarkus.qrs.runtime.spi;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.RequestContext;

/**
 * Extension of MessageBodyWriter which can write directly to a Vert.x response
 */
// FIXME: do we actually need to make it extend MessageBodyWriter?
public interface QrsMessageBodyWriter<T> extends MessageBodyWriter<T> {

    public void writeResponse(T o, RequestContext context) throws WebApplicationException;

}
