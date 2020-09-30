package io.quarkus.rest.runtime.core.serialization;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.util.HttpHeaderNames;
import io.quarkus.rest.runtime.util.MediaTypeHelper;

/**
 * Writer that is fully dynamic, and follows the spec defined resolution process
 */
public class DynamicEntityWriter implements EntityWriter {

    private final Serialisers serialisers;

    public DynamicEntityWriter(Serialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void write(QuarkusRestRequestContext context, Object entity) throws IOException {
        MediaType mt = context.getProducesMediaType();
        MessageBodyWriter<?>[] writers = null;
        if (mt == null) {
            MediaType selectedMediaType = null;
            if ((context.getTarget() != null) && (context.getTarget().getProduces() != null)) {
                MediaType res = context.getTarget().getProduces().negotiateProduces(context.getContext().request());
                List<MessageBodyWriter<?>> writersList = serialisers.findWriters(null, entity.getClass(), res,
                        RuntimeType.SERVER);
                if (!writersList.isEmpty()) {
                    writers = writersList.toArray(new MessageBodyWriter[0]);
                    selectedMediaType = res;
                }
            }
            if (writers == null) {
                Serialisers.NoMediaTypeResult writerNoMediaType = serialisers.findWriterNoMediaType(context, entity);
                writers = writerNoMediaType.getWriters();
                selectedMediaType = writerNoMediaType.getMediaType();
            }
            if (selectedMediaType != null) {
                if (MediaTypeHelper.isUnsupportedWildcardSubtype(selectedMediaType)) { // spec says the acceptable wildcard subtypes are */* or application/*
                    Serialisers.encodeResponseHeaders(context);
                    context.getContext().response().setStatusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
                    // spec says the response doesn't have a body so we just end the response here and return
                    context.getContext().response().end();
                    return;
                } else {
                    context.setProducesMediaType(selectedMediaType);
                    context.getContext().response().headers().add(HttpHeaderNames.CONTENT_TYPE, selectedMediaType.toString());
                }
            }
        } else {
            writers = serialisers.findWriters(null, entity.getClass(), mt, RuntimeType.SERVER).toArray(Serialisers.NO_WRITER);
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
