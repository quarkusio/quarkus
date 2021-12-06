package org.jboss.resteasy.reactive.server.handlers;

import java.util.Locale;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.jboss.resteasy.reactive.server.core.EncodedMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.serialization.EntityWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Handler that negotiates the content type for endpoints that
 * only produce a single type.
 */
public class FixedProducesHandler implements ServerRestHandler {

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
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        String accept = requestContext.serverRequest().getRequestHeader(HttpHeaders.ACCEPT);
        if (accept == null) {
            requestContext.setResponseContentType(mediaType);
            requestContext.setEntityWriter(writer);
        } else {
            //TODO: this needs to be optimised
            if (accept.contains(mediaTypeString) || accept.contains("*/*") || accept.contains(mediaTypeSubstring)) {
                requestContext.setResponseContentType(mediaType);
                requestContext.setEntityWriter(writer);
            } else {
                // some clients might be sending the header with incorrect casing...
                String lowercaseAccept = accept.toLowerCase(Locale.ROOT);
                if (lowercaseAccept.contains(mediaTypeString) || lowercaseAccept.contains(mediaTypeSubstring)) {
                    requestContext.setResponseContentType(mediaType);
                    requestContext.setEntityWriter(writer);
                } else {
                    throw new WebApplicationException(
                            Response.notAcceptable(Variant.mediaTypes(mediaType.getMediaType()).build()).build());
                }
            }
        }
    }
}
