package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import org.jboss.resteasy.reactive.DateFormat;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public abstract class TemporalParamConverter<T extends Temporal> implements ParameterConverter {

    private final DateTimeFormatter formatter;

    public TemporalParamConverter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    protected abstract T convert(String value);

    protected abstract T convert(String value, DateTimeFormatter formatter);

    @Override
    public Object convert(Object parameter) {
        String strValue = String.valueOf(parameter);
        if (formatter == null) {
            return convert(strValue);
        }
        return convert(strValue, formatter);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        // no init required
    }

    @SuppressWarnings("unused")
    public abstract static class TemporalSupplier<T extends TemporalParamConverter<?>>
            implements ParameterConverterSupplier {

        // class is mutable in order to make bytecode recording work
        private String pattern;
        private String dateTimeFormatterProviderClassName;

        public TemporalSupplier() {
        }

        public TemporalSupplier(String pattern, String dateTimeFormatterProviderClassName) {
            this.pattern = pattern;
            this.dateTimeFormatterProviderClassName = dateTimeFormatterProviderClassName;
        }

        protected abstract T createConverter(DateTimeFormatter dateTimeFormatter);

        @Override
        public ParameterConverter get() {
            DateTimeFormatter dateTimeFormatter = null;
            if (dateTimeFormatterProviderClassName != null) {
                try {
                    Class<?> formatterProviderClass = Class.forName(dateTimeFormatterProviderClassName, true,
                            Thread.currentThread().getContextClassLoader());
                    DateFormat.DateTimeFormatterProvider dateTimeFormatterProvider = (DateFormat.DateTimeFormatterProvider) formatterProviderClass
                            .getConstructor().newInstance();
                    dateTimeFormatter = dateTimeFormatterProvider.get();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to create instance of 'dateTimeFormatterProviderClassName'", e);
                }
            } else if (pattern != null) {
                dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            }

            return createConverter(dateTimeFormatter);
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getDateTimeFormatterProviderClassName() {
            return dateTimeFormatterProviderClassName;
        }

        public void setDateTimeFormatterProviderClassName(String dateTimeFormatterProviderClassName) {
            this.dateTimeFormatterProviderClassName = dateTimeFormatterProviderClassName;
        }
    }
}
