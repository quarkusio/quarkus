package io.quarkus.rest.runtime.core.parameters.converters;

public interface ParameterConverter {

    Object convert(Object parameter);
}
