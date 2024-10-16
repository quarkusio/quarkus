package io.quarkus.resteasy.reactive.server.test.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MyParameterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType == MyParameter.class)
            return (ParamConverter<T>) new MyParameterConverter();
        return null;
    }

}
