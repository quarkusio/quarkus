package io.quarkus.qrs.runtime.core.serialization;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.util.HttpHeaderNames;

/**
 * Writer that is fully dynamic, and follows the spec defined resolution process
 */
public class DynamicEntityWriter implements EntityWriter {

    private final Serialisers serialisers;

    public DynamicEntityWriter(Serialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void write(QrsRequestContext context, Object entity) throws IOException {
        MediaType mt = context.getProducesMediaType();
        MessageBodyWriter<?>[] writers;
        if (mt == null) {
            Serialisers.NoMediaTypeResult writerNoMediaType = serialisers.findWriterNoMediaType(context, entity);
            writers = writerNoMediaType.getWriters();
            context.setProducesMediaType(writerNoMediaType.getMediaType());
            context.getContext().response().headers().add(HttpHeaderNames.CONTENT_TYPE,
                    writerNoMediaType.getMediaType().toString());
        } else {
            writers = serialisers.findWriters(context, entity, mt);
        }
        for (MessageBodyWriter<?> w : writers) {
            if (Serialisers.invokeWriter(context, entity, w)) {
                return;
            }
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }
}
