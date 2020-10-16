package io.quarkus.rest.runtime.headers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.rest.runtime.core.EncodedMediaType;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.handlers.RestHandler;

/**
 * Handler that negotiates the content type for endpoints that
 * only produce a single type.
 */
public class FixedProducesHandler implements RestHandler {

    final EncodedMediaType mediaType;
    final String mediaTypeString;
    final String mediaTypeSubstring;
    final EntityWriter writer;

    public FixedProducesHandler(MediaType mediaType, EntityWriter writer) {
        this.mediaType = new EncodedMediaType(mediaType);
        this.writer = writer;
        this.mediaTypeString = mediaType.getType() + "/" + mediaType.getSubtype();
        this.mediaTypeSubstring = mediaType.getType() + "/*";
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        if (accept == null) {
            requestContext.setResponseContentType(mediaType);
            requestContext.setEntityWriter(writer);
        } else {
            //TODO: this needs to be optimised
            if (accept.contains(mediaTypeString) || accept.contains("*/*") || accept.contains(mediaTypeSubstring)) {
                requestContext.setResponseContentType(mediaType);
                requestContext.setEntityWriter(writer);
            } else {
                throw new WebApplicationException(
                        Response.notAcceptable(Variant.mediaTypes(mediaType.getMediaType()).build()).build());
            }
        }
    }
}
