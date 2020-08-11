package io.quarkus.qrs.runtime.headers;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyWriter;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;

/**
 * Handler that negotiates the content type for endpoints that
 * have multiple produces types, or for whatever reason can't have
 * their writer list and media type determined at build time.
 */
public class VariableProducesHandler implements RestHandler {

    public static final MessageBodyWriter[] EMPTY = new MessageBodyWriter[0];
    final List<MediaType> mediaTypeList;
    final Serialisers serialisers;

    public VariableProducesHandler(List<MediaType> mediaTypeList, Serialisers serialisers) {
        this.mediaTypeList = mediaTypeList;
        this.serialisers = serialisers;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        Object entity = requestContext.getResult();
        if (entity instanceof Response) {
            return;
        }
        if (entity == null) {
            //TODO?
            return;
        }
        if (accept == null) {
            requestContext.setProducesMediaType(mediaTypeList.get(0));
        } else {
            //TODO: this needs to be optimised, its super inefficient
            List<MediaType> parsed = MediaTypeHelper.parseHeader(accept);
            MediaType res = MediaTypeHelper.getBestMatch(parsed, mediaTypeList);
            if (res == null) {
                requestContext.setThrowable(new WebApplicationException(Response
                        .notAcceptable(Variant.mediaTypes(mediaTypeList.toArray(new MediaType[mediaTypeList.size()])).build())
                        .build()));
                return;
            }
            List<MessageBodyWriter<?>> writers = serialisers.findWriters(entity.getClass(), res);
            if (writers == null || writers.isEmpty()) {
                requestContext.setThrowable(new WebApplicationException(Response
                        .notAcceptable(Variant.mediaTypes(mediaTypeList.toArray(new MediaType[mediaTypeList.size()])).build())
                        .build()));
                return;
            }
            requestContext.setProducesMediaType(res);
            requestContext.setEntityWriter(new FixedEntityWriterArray(writers.toArray(EMPTY)));
        }
    }
}
