package io.quarkus.qrs.runtime.handlers;

import javax.ws.rs.core.Response;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;

/**
 * Handler that negotiates the content type for endpoints that do not specify @Produces
 */
public class NoProducesHandler implements RestHandler {

    private final Serialisers serialisers;

    public NoProducesHandler(Serialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        Object entity = requestContext.getResult();
        if (entity instanceof Response || entity == null) {
            return;
        }
        Serialisers.NoMediaTypeResult result = serialisers.findWriterNoMediaType(requestContext, entity);
        requestContext.setProducesMediaType(result.getMediaType());
        requestContext.setEntityWriter(result.getEntityWriter());
    }

}
