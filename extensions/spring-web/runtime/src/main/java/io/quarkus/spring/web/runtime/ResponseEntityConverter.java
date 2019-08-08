package io.quarkus.spring.web.runtime;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class ResponseEntityConverter {

    public static Response toResponse(ResponseEntity responseEntity) {
        return new BuiltResponse(
                responseEntity.getStatusCodeValue(), toHeaders(responseEntity.getHeaders()), responseEntity.getBody(),
                new Annotation[0]);
    }

    private static Headers<Object> toHeaders(HttpHeaders springHeaders) {
        Headers<Object> jaxRsHeaders = new Headers<>();
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            jaxRsHeaders.addAll(entry.getKey(), entry.getValue());
        }
        return jaxRsHeaders;
    }

}
