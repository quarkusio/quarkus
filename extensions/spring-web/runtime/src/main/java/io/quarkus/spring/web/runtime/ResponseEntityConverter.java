package io.quarkus.spring.web.runtime;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;
import org.jboss.resteasy.reactive.server.jaxrs.ResponseBuilderImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * This is only used in the generated ExceptionMappers when the Spring @RestControllerAdvice method returns a ResponseEntity
 */
public class ResponseEntityConverter {

    public static final String[] EMPTY_STRINGS_ARRAY = new String[0];

    public static Response toResponse(ResponseEntity<?> responseEntity) {
        ResponseBuilderImpl responseBuilder = toResponseBuilder(responseEntity);
        return responseBuilder.build();
    }

    @SuppressWarnings("rawtypes")
    public static Response toResponse(ResponseEntity responseEntity, MediaType defaultMediaType) {
        ResponseBuilderImpl responseBuilder = toResponseBuilder(responseEntity);
        if (!responseBuilder.getMetadata().containsKey(HttpHeaders.CONTENT_TYPE)) {
            responseBuilder.header(HttpHeaders.CONTENT_TYPE, defaultMediaType.toString());
        }
        return responseBuilder.build();
    }

    private static ResponseBuilderImpl toResponseBuilder(ResponseEntity<?> responseEntity) {
        ResponseBuilderImpl responseBuilder = new ResponseBuilderImpl();
        responseBuilder.status(responseEntity.getStatusCodeValue()).entity(responseEntity.getBody());
        responseBuilder.setAllHeaders(toJaxRsHeaders(responseEntity.getHeaders()));
        return responseBuilder;
    }

    private static MultivaluedMap<String, String> toJaxRsHeaders(HttpHeaders springHeaders) {
        var jaxRsHeaders = new MultivaluedTreeMap<String, String>();
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            jaxRsHeaders.addAll(entry.getKey(), entry.getValue().toArray(EMPTY_STRINGS_ARRAY));
        }
        return jaxRsHeaders;
    }
}
