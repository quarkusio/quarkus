package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ConfigurableExceptionMapper
        implements ExceptionMapper<ConfigurableExceptionMapper.ConfigurableExceptionMapperException> {
    @Inject
    @ConfigProperty(name = "exception.message")
    String message;
    @Inject
    ExceptionConfig exceptionConfig;

    @Override
    public Response toResponse(final ConfigurableExceptionMapperException exception) {
        if (!message.equals(exceptionConfig.message())) {
            return Response.serverError().build();
        }

        return Response.ok().entity(message).build();
    }

    public static class ConfigurableExceptionMapperException extends RuntimeException {

    }
}
