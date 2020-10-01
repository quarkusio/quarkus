package io.quarkus.rest.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.ParamConverterProviders;

public interface InitRequiredParameterConverter extends ParameterConverter {

    void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations);

    @SuppressWarnings("unused")
    static void handleFieldInit(InitRequiredParameterConverter converter, String fieldName, String declaringClass) {
        try {
            Class<?> clazz = Class.forName(declaringClass, false, Thread.currentThread().getContextClassLoader());
            Field field = clazz.getDeclaredField(fieldName);
            converter.init(QuarkusRestRecorder.getCurrentDeployment().getParamConverterProviders(), field.getType(),
                    field.getGenericType(), field.getDeclaredAnnotations());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
