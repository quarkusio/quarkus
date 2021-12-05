package org.jboss.resteasy.reactive.server.providers.serialisers.json;

import static org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil.isNotJson;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public final class JsonMessageServerBodyWriterUtil {

    private JsonMessageServerBodyWriterUtil() {
    }

    public static void setContentTypeIfNecessary(ServerRequestContext context) {
        String currentContentType = context.serverResponse().getResponseHeader(HttpHeaders.CONTENT_TYPE);
        if (isNotJson(currentContentType)) {
            context.serverResponse().setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
    }
}
