package io.quarkus.spring.web.runtime;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * This is only used in the generated ExceptionMappers when the Spring @RestControllerAdvice method returns a ResponseEntity
 */
public class ResponseEntityConverter {

    public static Response toResponse(ResponseEntity responseEntity, MediaType defaultContentType) {
        return new BuiltResponse(responseEntity.getStatusCodeValue(),
                addContentTypeIfMissing(toJaxRsHeaders(responseEntity.getHeaders()), defaultContentType),
                responseEntity.getBody(),
                new Annotation[0]);
    }

    private static Headers<Object> toJaxRsHeaders(HttpHeaders springHeaders) {
        Headers<Object> jaxRsHeaders = new Headers<>();
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            jaxRsHeaders.addAll(entry.getKey(), entry.getValue().toArray(new Object[0]));
        }
        return jaxRsHeaders;
    }

    private static Headers<Object> addContentTypeIfMissing(Headers<Object> headers, MediaType contentType) {
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return headers;
    }
}
