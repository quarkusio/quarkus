package io.quarkus.resteasy.jackson.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ObjectMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper() {
        // in the future we can do a lot more here
        return new ObjectMapper();
    }
}
