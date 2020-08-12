package io.quarkus.qrs.runtime.core.serialization;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;

/**
 * A fixed entity writer that can be used when we know the result will always be written
 * by a given provider.
 */
public class FixedEntityWriter implements EntityWriter {

    private final MessageBodyWriter writer;

    public FixedEntityWriter(MessageBodyWriter writer, MediaType mediaType) {
        this.writer = writer;
    }

    @Override
    public void write(QrsRequestContext context, Object entity) throws IOException {
        if (!Serialisers.invokeWriter(context, entity, writer)) {
            throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                    Response.serverError().build());
        }
    }

}
