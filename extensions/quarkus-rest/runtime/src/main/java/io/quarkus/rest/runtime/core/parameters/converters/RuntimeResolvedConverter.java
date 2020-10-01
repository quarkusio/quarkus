package io.quarkus.rest.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.runtime.core.ParamConverterProviders;
import io.quarkus.rest.runtime.model.ResourceParamConverterProvider;

public class RuntimeResolvedConverter implements InitRequiredParameterConverter {

    private volatile ParamConverter<?> converter;

    @Override
    public Object convert(Object parameter) {
        return converter.fromString(parameter.toString());
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        for (ResourceParamConverterProvider i : deployment.getParamConverterProviders()) {
            ParamConverterProvider instance = i.getFactory().createInstance().getInstance();
            ParamConverter<?> converter = instance.getConverter(rawType, genericType, annotations);
            if (converter != null) {
                this.converter = converter;
                break;
            }
        }
        if (converter == null) {
            throw new RuntimeException("Unable to create param converter for parameter " + rawType);
        }
    }

    public static class Supplier implements ParameterConverterSupplier {

        int index;

        public int getIndex() {
            return index;
        }

        public Supplier setIndex(int index) {
            this.index = index;
            return this;
        }

        @Override
        public String getClassName() {
            return RuntimeResolvedConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return new RuntimeResolvedConverter();
        }
    }
}
