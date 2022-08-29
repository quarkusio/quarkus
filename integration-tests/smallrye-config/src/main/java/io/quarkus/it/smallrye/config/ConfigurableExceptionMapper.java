package io.quarkus.it.smallrye.config;

import javax.enterprise.inject.Instance;
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
