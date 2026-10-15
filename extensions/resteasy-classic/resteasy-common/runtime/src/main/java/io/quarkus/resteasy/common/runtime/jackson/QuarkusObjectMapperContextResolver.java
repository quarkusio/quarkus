package io.quarkus.resteasy.common.runtime.jackson;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 10) // give it a priority that ensures that user supplied ContextResolver classes override this one
public class QuarkusObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final JsonMapper jsonMapper;

    public QuarkusObjectMapperContextResolver(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return jsonMapper;
    }
}
