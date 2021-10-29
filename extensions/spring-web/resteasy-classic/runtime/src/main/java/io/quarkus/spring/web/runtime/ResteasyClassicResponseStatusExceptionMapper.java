package io.quarkus.spring.web.runtime;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.springframework.web.server.ResponseStatusException;

import io.quarkus.spring.web.runtime.common.AbstractResponseStatusExceptionMapper;

public class ResteasyClassicResponseStatusExceptionMapper extends AbstractResponseStatusExceptionMapper {

    @Override
    protected Response.ResponseBuilder createResponseBuilder(ResponseStatusException exception) {
        return new ResponseBuilderImpl().status(exception.getStatus().value());
    }
}
