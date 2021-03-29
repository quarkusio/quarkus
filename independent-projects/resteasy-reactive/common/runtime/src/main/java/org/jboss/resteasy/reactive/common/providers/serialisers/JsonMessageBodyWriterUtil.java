package org.jboss.resteasy.reactive.common.providers.serialisers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class JsonMessageBodyWriterUtil {
    public static void setContentTypeIfNecessary(MultivaluedMap<String, Object> httpHeaders) {
        Object contentType = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (isNotJson(contentType)) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
    }

    public static boolean isNotJson(Object contentType) {
        return (contentType == null) || !contentType.toString().contains("json");
    }
}
