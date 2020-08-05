package io.quarkus.qrs.runtime.core.serialization;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;

/**
 * Writer that is fully dynamic, and follows the spec defined resolution process
 */
public class DynamicEntityWriter implements EntityWriter {

    private final Serialisers serialisers;

    public DynamicEntityWriter(Serialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void write(RequestContext context, Object entity) throws IOException {
        MessageBodyWriter<?> writer = serialisers.findWriter(context.getResponse(), context);
        if (writer == null) {
            throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                    Response.serverError().build());
        }
        FixedEntityWriter.invokeWriter(context, entity, writer);
    }
}
