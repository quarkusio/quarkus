package org.jboss.resteasy.reactive.server.handlers;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Handler that defines the default content type when a Response is returned.
 * While it might not be the final content type, we still need to make sure
 * the default content type is provided to {@code ContainerResponseFilter}.
 * <p>
 * This particular one negotiates the content type when there are multiple ones defined.
 */
public class VariableProducesSetDefaultContentTypeResponseHandler implements ServerRestHandler {

    private final ServerMediaType mediaTypeList;

    public VariableProducesSetDefaultContentTypeResponseHandler(ServerMediaType mediaTypeList) {
        this.mediaTypeList = mediaTypeList;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        MediaType res = null;
        List<String> accepts = requestContext.getHttpHeaders().getRequestHeader(HttpHeaders.ACCEPT);
        for (String accept : accepts) {
            res = mediaTypeList.negotiateProduces(accept).getKey();
            if (res != null) {
                break;
            }
        }
        if (res == null) { // fallback to ensure that MessageBodyWriter is passed the proper media type
            res = mediaTypeList.negotiateProduces(requestContext.serverRequest().getRequestHeader(HttpHeaders.ACCEPT))
                    .getKey();
        }

        if (res == null || MediaTypeHelper.isUnsupportedWildcardSubtype(res)) {
            return;
        }

        requestContext.setResponseContentType(res);
    }
}
