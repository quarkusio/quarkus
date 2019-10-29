package io.quarkus.jackson;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ObjectMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule());
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(objectMapper);
        }
        return objectMapper;
    }
}
