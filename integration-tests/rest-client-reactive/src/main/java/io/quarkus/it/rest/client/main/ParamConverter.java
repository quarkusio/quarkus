package io.quarkus.it.rest.client.main;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverterProvider;

public class ParamConverter implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> jakarta.ws.rs.ext.ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
            Annotation[] annotations) {
        if (genericType == null || !genericType.equals(Param.class)) {
            throw new RuntimeException("Wrong generic type in ParamConverter!");
        }

        if (annotations == null || annotations.length != 1 || !(annotations[0] instanceof QueryParam)) {
            throw new RuntimeException("Wrong annotations in ParamConverter!");
        }

        if (rawType == Param.class) {
            return (jakarta.ws.rs.ext.ParamConverter<T>) new jakarta.ws.rs.ext.ParamConverter<Param>() {
                @Override
                public Param fromString(String value) {
                    return null;
                }

                @Override
                public String toString(Param value) {
                    if (value == null) {
                        return null;
                    }
                    switch (value) {
                        case FIRST:
                            return "1";
                        case SECOND:
                            return "2";
                        default:
                            return "unexpected";
                    }
                }
            };
        }
        return null;
    }
}
