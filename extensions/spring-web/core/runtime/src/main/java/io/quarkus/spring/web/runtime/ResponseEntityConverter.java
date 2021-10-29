package io.quarkus.spring.web.runtime;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.MultivaluedTreeMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * This is only used in the generated ExceptionMappers when the Spring @RestControllerAdvice method returns a ResponseEntity
 */
public class ResponseEntityConverter {

    private static final String[] EMPTY_STRINGS_ARRAY = new String[0];

    @SuppressWarnings("rawtypes")
    public static Response toResponse(ResponseEntity responseEntity, MediaType defaultMediaType) {
        Response.ResponseBuilder responseBuilder = Response.status(responseEntity.getStatusCodeValue())
                .entity(responseEntity.getBody());
        MultivaluedMap<String, String> jaxRsHeaders = toJaxRsHeaders(responseEntity.getHeaders());
        if (!jaxRsHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            jaxRsHeaders.putSingle(HttpHeaders.CONTENT_TYPE, defaultMediaType.toString());
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

    private static MultivaluedMap<String, String> toJaxRsHeaders(HttpHeaders springHeaders) {
        var jaxRsHeaders = new MultivaluedTreeMap<String, String>();
        for (var entry : springHeaders.entrySet()) {
            jaxRsHeaders.addAll(entry.getKey(), entry.getValue().toArray(EMPTY_STRINGS_ARRAY));
        }
        return jaxRsHeaders;
    }
}
