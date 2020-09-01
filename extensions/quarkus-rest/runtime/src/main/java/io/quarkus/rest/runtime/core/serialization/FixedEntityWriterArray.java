package io.quarkus.rest.runtime.core.serialization;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;

/**
 * A fixed entity writer that iterates an array of providers until it finds one that can handle
 * the given types.
 */
public class FixedEntityWriterArray implements EntityWriter {

    private final MessageBodyWriter[] writers;

    public FixedEntityWriterArray(MessageBodyWriter[] writers) {
        this.writers = writers;
    }

    @Override
    public void write(QuarkusRestRequestContext context, Object entity) throws IOException {
        for (int i = 0; i < writers.length; ++i) {
            MessageBodyWriter writer = writers[i];
            if (Serialisers.invokeWriter(context, entity, writer)) {
                return;
            }
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }
}
