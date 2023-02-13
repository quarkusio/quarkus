package io.quarkus.resteasy.common.runtime.jackson;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 10) // give it a priority that ensures that user supplied ContextResolver classes override this one
public class QuarkusObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
