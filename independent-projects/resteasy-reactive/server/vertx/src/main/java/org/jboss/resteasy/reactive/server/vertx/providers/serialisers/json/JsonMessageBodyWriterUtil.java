package org.jboss.resteasy.reactive.server.vertx.providers.serialisers.json;

import io.vertx.core.MultiMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public final class JsonMessageBodyWriterUtil {

    private JsonMessageBodyWriterUtil() {
    }

    public static void setContentTypeIfNecessary(MultivaluedMap<String, Object> httpHeaders) {
        Object contentType = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (isNotJson(contentType)) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
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

    private static boolean isNotJson(Object contentType) {
        return (contentType == null) || !contentType.toString().contains("json");
    }
}
