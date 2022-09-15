package org.jboss.resteasy.reactive.server.core.serialization;

import java.io.IOException;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

/**
 * An interface that can be used to write out an entity.
 *
 * In practical terms these represent a set of {@link jakarta.ws.rs.ext.MessageBodyWriter}
 * implementations. As must as possible the implementations are resolved at build time, however
 * the spec does allow for dynamic behaviour, which is abstracted behind this interface.
 *
 */
public interface EntityWriter {

    void write(ResteasyReactiveRequestContext context, Object entity) throws IOException;

}
