package io.quarkus.spring.web.runtime.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * This is only used in the generated ExceptionMappers when the Spring @RestControllerAdvice method returns a ResponseEntity
 */
public class ResponseEntityConverter {

    @SuppressWarnings("rawtypes")
    public static Response toResponse(ResponseEntity responseEntity, MediaType defaultMediaType) {
        Response.ResponseBuilder responseBuilder = Response.status(responseEntity.getStatusCodeValue())
                .entity(responseEntity.getBody());
        var jaxRsHeaders = toJaxRsHeaders(responseEntity.getHeaders());
        if (!jaxRsHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && (defaultMediaType != null)) {
            jaxRsHeaders.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(defaultMediaType.toString()));
        }
        for (var entry : jaxRsHeaders.entrySet()) {
            var value = entry.getValue();
            if (value.size() == 1) {
                responseBuilder.header(entry.getKey(), entry.getValue().get(0));
            } else {
                responseBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        return responseBuilder.build();
    }

    private static Map<String, List<String>> toJaxRsHeaders(HttpHeaders springHeaders) {
        return new HashMap<>(springHeaders);
    }
}
