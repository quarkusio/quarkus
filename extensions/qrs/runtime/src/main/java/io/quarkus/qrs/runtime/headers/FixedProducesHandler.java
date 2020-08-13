package io.quarkus.qrs.runtime.headers;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;

/**
 * Handler that negotiates the content type for endpoints that
 * only produce a single type.
 */
public class FixedProducesHandler implements RestHandler {

    final MediaType mediaType;
    final EntityWriter writer;
    final List<MediaType> mediaTypeList;

    public FixedProducesHandler(MediaType mediaType, EntityWriter writer) {
        this.mediaType = mediaType;
        this.writer = writer;
        this.mediaTypeList = Collections.singletonList(mediaType);
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        if (accept == null) {
            requestContext.setProducesMediaType(mediaType);
            requestContext.setEntityWriter(writer);
        } else {
            //TODO: this needs to be optimised
            List<MediaType> parsed = MediaTypeHelper.parseHeader(accept);
            MediaType res = MediaTypeHelper.getBestMatch(parsed, mediaTypeList);
            if (res == null) {
                throw new WebApplicationException(Response.notAcceptable(Variant.mediaTypes(mediaType).build()).build());
            }
            requestContext.setProducesMediaType(res);
            requestContext.setEntityWriter(writer);
        }
    }
}
