package org.jboss.resteasy.reactive.server.handlers;

import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.EncodedMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Handler that defines the default content type when a Response is returned.
 * While it might not be the final content type, we still need to make sure
 * the default content type is provided to {@code ContainerResponseFilter}.
 * <p>
 * This particular one is for endpoints that only produce one content type.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class FixedProducesSetDefaultContentTypeResponseHandler implements ServerRestHandler {

    private final EncodedMediaType mediaType;
    private final String mediaTypeString;
    private final String mediaTypeSubstring;

    public FixedProducesSetDefaultContentTypeResponseHandler(MediaType mediaType) {
        this.mediaType = new EncodedMediaType(mediaType);
        this.mediaTypeString = mediaType.getType() + "/" + mediaType.getSubtype();
        this.mediaTypeSubstring = mediaType.getType() + "/*";
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        List<String> acceptValues;
        if (requestContext.isProducesChecked() ||
                (acceptValues = (List<String>) requestContext.getHeader(HttpHeaders.ACCEPT, false)).isEmpty()) {
            requestContext.setResponseContentType(mediaType);
        } else {
            for (int i = 0; i < acceptValues.size(); i++) {
                String accept = acceptValues.get(i);
                //TODO: this needs to be optimized
                if (accept.contains(mediaTypeString) || accept.contains("*/*") || accept.contains(mediaTypeSubstring)) {
                    requestContext.setResponseContentType(mediaType);
                    break;
                } else {
                    // some clients might be sending the header with incorrect casing...
                    String lowercaseAccept = accept.toLowerCase(Locale.ROOT);
                    if (lowercaseAccept.contains(mediaTypeString) || lowercaseAccept.contains(mediaTypeSubstring)) {
                        requestContext.setResponseContentType(mediaType);
                        break;
                    }
                }
            }
        }
    }
}
