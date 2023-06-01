package io.quarkus.it.resteasy.jackson;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JacksonConfiguration implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE);
    }

}
