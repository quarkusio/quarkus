package io.quarkus.observability.promql.client.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.observability.promql.client.data.Dur;

@Provider
@ConstrainedTo(RuntimeType.CLIENT)
public class PromQLParamConverterProvider extends InstantParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        var converter = super.getConverter(rawType, genericType, annotations);
        if (converter != null) {
            return converter;
        }
        if (Dur.class.isAssignableFrom(rawType)) {
            return cast(rawType, DUR_PARAM_CONVERTER);
        }
        return null;
    }

    private static final ParamConverter<Dur> DUR_PARAM_CONVERTER = new PC<>(
            string -> {
                throw new UnsupportedOperationException("Parsing of Dur not implemented yet.");
            },
            Dur::toString);
}
