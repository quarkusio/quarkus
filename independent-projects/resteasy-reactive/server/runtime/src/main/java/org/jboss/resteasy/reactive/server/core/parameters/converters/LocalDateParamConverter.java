package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class LocalDateParamConverter implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        return LocalDate.parse(String.valueOf(parameter), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType,
            Annotation[] annotations) {
        // no init required
    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public String getClassName() {
            return LocalDateParamConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return new LocalDateParamConverter();
        }
    }
}
