package io.quarkus.spring.web.resteasy.classic.runtime;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class ResponseEntityContainerResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Object entity = responseContext.getEntity();
        if (!(entity instanceof ResponseEntity)) {
            return;
        }
        ResponseEntity<?> responseEntity = (ResponseEntity<?>) entity;
        responseContext.setStatus(responseEntity.getStatusCode().value());
        responseContext.setEntity(responseEntity.getBody());
        HttpHeaders headers = responseEntity.getHeaders();
        for (String name : headers.headerNames()) {
            List<String> values = headers.getValuesAsList(name);
            responseContext.getHeaders().addAll(name, values.toArray(new Object[0]));
        }
    }
}
