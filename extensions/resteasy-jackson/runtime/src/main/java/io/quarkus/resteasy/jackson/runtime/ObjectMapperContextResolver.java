package io.quarkus.resteasy.jackson.runtime;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return Arc.container().instance(ObjectMapper.class).get();
    }
}
