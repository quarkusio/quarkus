package org.jboss.resteasy.reactive.server.core.serialization;

import java.io.IOException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;

/**
 * A fixed entity writer that iterates an array of providers until it finds one that can handle
 * the given types.
 */
public class FixedEntityWriterArray implements EntityWriter {

    private final MessageBodyWriter[] writers;
    private final ServerSerialisers serialisers;

    public FixedEntityWriterArray(MessageBodyWriter[] writers, ServerSerialisers serialisers) {
        this.writers = writers;
        this.serialisers = serialisers;
    }

    @Override
    public void write(ResteasyReactiveRequestContext context, Object entity) throws IOException {
        for (int i = 0; i < writers.length; ++i) {
            MessageBodyWriter writer = writers[i];
            if (ServerSerialisers.invokeWriter(context, entity, writer, serialisers)) {
                return;
            }
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }
}
