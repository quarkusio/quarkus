package io.quarkus.qrs.runtime.core.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

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
    public void write(RequestContext context, Object entity) throws IOException {
        Response response = context.getResponse();
        for (int i = 0; i < writers.length; ++i) {
            MessageBodyWriter writer = writers[i];
            if (writer.isWriteable(entity.getClass(), null, null, response.getMediaType())) {
                if (writer instanceof QrsMessageBodyWriter) {
                    ((QrsMessageBodyWriter<Object>) writer).writeResponse(entity, context);
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entity, null, null, null, response.getMediaType(), null, baos);
                    context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
                }
                return;
            }
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }
}
