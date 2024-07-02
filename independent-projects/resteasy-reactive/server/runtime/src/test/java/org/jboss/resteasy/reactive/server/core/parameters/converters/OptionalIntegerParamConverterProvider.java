package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class OptionalIntegerParamConverterProvider implements ParamConverterProvider {
	
	public OptionalIntegerParamConverterProvider() {
		super();
	}
	
    @SuppressWarnings("unchecked")
	@Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    	if (rawType.equals(Optional.class)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length == 1 && typeArguments[0].equals(Integer.class)) {
                    return (ParamConverter<T>) new OptionalIntegerParamConverter();
                }
            }
        }
        
        return null;
    }
}
