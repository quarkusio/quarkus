package org.jboss.resteasy.reactive.server.handlers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import java.util.List;
import java.util.Locale;
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
        List<String> acceptValues = (List<String>) requestContext.getHeader(HttpHeaders.ACCEPT, false);
        if (acceptValues.isEmpty()) {
            requestContext.setResponseContentType(mediaType);
            requestContext.setEntityWriter(writer);
        } else {
            boolean handled = false;
            for (int i = 0; i < acceptValues.size(); i++) {
                String accept = acceptValues.get(i);
                //TODO: this needs to be optimised
                if (accept.contains(mediaTypeString) || accept.contains("*/*") || accept.contains(mediaTypeSubstring)) {
                    requestContext.setResponseContentType(mediaType);
                    requestContext.setEntityWriter(writer);
                    handled = true;
                    break;
                } else {
                    // some clients might be sending the header with incorrect casing...
                    String lowercaseAccept = accept.toLowerCase(Locale.ROOT);
                    if (lowercaseAccept.contains(mediaTypeString) || lowercaseAccept.contains(mediaTypeSubstring)) {
                        requestContext.setResponseContentType(mediaType);
                        requestContext.setEntityWriter(writer);
                        handled = true;
                        break;
                    }
                }
            }
            if (!handled) {
                throw new WebApplicationException(
                        Response.notAcceptable(Variant.mediaTypes(mediaType.getMediaType()).build()).build());
            }
        }
    }
}
