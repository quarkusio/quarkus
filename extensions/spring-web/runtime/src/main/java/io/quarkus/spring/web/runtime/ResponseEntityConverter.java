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

    public static Response toResponse(ResponseEntity responseEntity, boolean addJsonContentTypeIfNotSet) {
        return new BuiltResponse(
                responseEntity.getStatusCodeValue(), toHeaders(responseEntity.getHeaders(), addJsonContentTypeIfNotSet),
                responseEntity.getBody(),
                new Annotation[0]);
    }

    private static Headers<Object> toHeaders(HttpHeaders springHeaders, boolean addJsonContentTypeIfNotSet) {
        Headers<Object> jaxRsHeaders = new Headers<>();
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            jaxRsHeaders.addAll(entry.getKey(), entry.getValue().toArray(new Object[0]));
        }
        /*
         * We add the default application/json content type if no content type is specified
         * since this is the default value when returning an object from a Spring RestController
         */
        if (addJsonContentTypeIfNotSet && !jaxRsHeaders.containsKey(javax.ws.rs.core.HttpHeaders.CONTENT_TYPE)) {
            jaxRsHeaders.add(javax.ws.rs.core.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
        return jaxRsHeaders;
    }

}
