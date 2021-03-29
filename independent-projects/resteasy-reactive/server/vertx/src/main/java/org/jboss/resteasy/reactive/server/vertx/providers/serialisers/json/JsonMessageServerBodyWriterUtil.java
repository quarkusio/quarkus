package org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json;

import static org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil.isNotJson;

import io.vertx.core.MultiMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public final class JsonMessageServerBodyWriterUtil {

    private JsonMessageServerBodyWriterUtil() {
    }

    public static void setContentTypeIfNecessary(ServerRequestContext context) {
        String currentContentType = null;
        Iterable<Map.Entry<String, String>> responseHeaders = context.serverResponse().getAllResponseHeaders();
        if (responseHeaders instanceof MultiMap) {
            currentContentType = ((MultiMap) responseHeaders).get(HttpHeaders.CONTENT_TYPE);
        } else {
            for (Map.Entry<String, String> entry : responseHeaders) {
                if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                    currentContentType = entry.getValue();
                    break;
                }
            }
        }
        if (isNotJson(currentContentType)) {
            context.serverResponse().setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
    }
}
