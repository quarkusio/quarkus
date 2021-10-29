package io.quarkus.spring.web.runtime.common;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

public class ResponseStatusExceptionMapper implements ExceptionMapper<ResponseStatusException> {

    @Override
    public Response toResponse(ResponseStatusException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(exception.getStatus().value());
        addHeaders(responseBuilder, exception.getResponseHeaders());
        return responseBuilder.entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN).build();
    }

    private void addHeaders(Response.ResponseBuilder responseBuilder, HttpHeaders springHeaders) {
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            for (String headerValue : entry.getValue()) {
                responseBuilder.header(entry.getKey(), headerValue);
            }
        }
    }
}
