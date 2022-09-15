package org.jboss.resteasy.reactive.server.core.serialization;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;

/**
 * A fixed entity writer that can be used when we know the result will always be written
 * by a given provider.
 */
public class FixedEntityWriter implements EntityWriter {

    private final MessageBodyWriter writer;
    private final ServerSerialisers serialisers;

    public FixedEntityWriter(MessageBodyWriter writer, ServerSerialisers serialisers) {
        this.writer = writer;
        this.serialisers = serialisers;
    }

    @Override
    public void write(ResteasyReactiveRequestContext context, Object entity) throws IOException {
        if (!ServerSerialisers.invokeWriter(context, entity, writer, serialisers)) {
            throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                    Response.serverError().build());
        }
    }

}
