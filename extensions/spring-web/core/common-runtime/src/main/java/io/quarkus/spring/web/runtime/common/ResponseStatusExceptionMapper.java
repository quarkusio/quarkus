package io.quarkus.spring.web.runtime.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

public class ResponseStatusExceptionMapper implements ExceptionMapper<ResponseStatusException> {

    @Override
    public Response toResponse(ResponseStatusException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(exception.getStatusCode().value());
        addHeaders(responseBuilder, exception.getHeaders());
        return responseBuilder.entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN).build();
    }

    private void addHeaders(Response.ResponseBuilder responseBuilder, HttpHeaders springHeaders) {
        for (String name : springHeaders.headerNames()) {
            for (String headerValue : springHeaders.getValuesAsList(name)) {
                responseBuilder.header(name, headerValue);
            }
        }
    }
}
