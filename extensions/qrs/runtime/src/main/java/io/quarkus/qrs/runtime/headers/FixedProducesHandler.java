package io.quarkus.qrs.runtime.headers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.handlers.RestHandler;

/**
 * Handler that negotiates the content type for endpoints that
 * only produce a single type.
 */
public class FixedProducesHandler implements RestHandler {

    final MediaType mediaType;
    final String mediaTypeString;
    final String mediaTypeSubstring;
    final EntityWriter writer;

    public FixedProducesHandler(MediaType mediaType, EntityWriter writer) {
        this.mediaType = mediaType;
        this.writer = writer;
        this.mediaTypeString = mediaType.getType() + "/" + mediaType.getSubtype();
        this.mediaTypeSubstring = mediaType.getType() + "/*";
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        if (accept == null) {
            requestContext.setProducesMediaType(mediaType);
            requestContext.setEntityWriter(writer);
        } else {
            //TODO: this needs to be optimised
            if (accept.contains(mediaTypeString) || accept.contains("*/*") || accept.contains(mediaTypeSubstring)) {
                requestContext.setProducesMediaType(mediaType);
                requestContext.setEntityWriter(writer);
            } else {
                throw new WebApplicationException(Response.notAcceptable(Variant.mediaTypes(mediaType).build()).build());
            }
        }
    }
}
