package io.quarkus.it.smallrye.config;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ConfigurableExceptionMapper
        implements ExceptionMapper<ConfigurableExceptionMapper.ConfigurableExceptionMapperException> {
    @Inject
    @ConfigProperty(name = "exception.message")
    Instance<String> message;
    @Inject
    Instance<ExceptionConfig> exceptionConfig;

    @Override
    public Response toResponse(final ConfigurableExceptionMapperException exception) {
        if (!message.get().equals(exceptionConfig.get().message())) {
            return Response.serverError().build();
        }

        return Response.ok().entity(message.get()).build();
    }

    public static class ConfigurableExceptionMapperException extends RuntimeException {

    }
}
