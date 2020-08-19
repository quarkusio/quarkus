package io.quarkus.qrs.runtime.headers;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.util.ServerMediaType;

/**
 * Handler that negotiates the content type for endpoints that
 * have multiple produces types, or for whatever reason can't have
 * their writer list and media type determined at build time.
 */
public class VariableProducesHandler implements RestHandler {

    public static final MessageBodyWriter[] EMPTY = new MessageBodyWriter[0];
    final ServerMediaType mediaTypeList;
    final Serialisers serialisers;

    public VariableProducesHandler(ServerMediaType mediaTypeList, Serialisers serialisers) {
        this.mediaTypeList = mediaTypeList;
        this.serialisers = serialisers;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        Object entity = requestContext.getResult();
        if (entity instanceof Response) {
            return;
        }
        if (entity == null) {
            //TODO?
            return;
        }
        MediaType res = mediaTypeList.negotiateProduces(requestContext.getContext().request());
        if (res == null) {
            throw new WebApplicationException(Response
                    .notAcceptable(Variant.mediaTypes(mediaTypeList.getSortedMediaTypes()).build())
                    .build());
        }
        List<MessageBodyWriter<?>> writers = serialisers.findWriters(entity.getClass(), res);
        if (writers == null || writers.isEmpty()) {
            throw new WebApplicationException(Response
                    .notAcceptable(Variant.mediaTypes(mediaTypeList.getSortedMediaTypes()).build())
                    .build());
        }
        requestContext.setProducesMediaType(res);
        requestContext.setEntityWriter(new FixedEntityWriterArray(writers.toArray(EMPTY)));
    }
}
