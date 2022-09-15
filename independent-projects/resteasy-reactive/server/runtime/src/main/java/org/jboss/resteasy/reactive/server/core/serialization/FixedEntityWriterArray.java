package org.jboss.resteasy.reactive.server.core.serialization;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
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
