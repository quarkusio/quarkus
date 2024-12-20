package io.quarkus.spring.web.resteasy.reactive.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MapParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType == Map.class) {
            return (ParamConverter<T>) new MapParamConverter();
        }
        return null;
    }
}
