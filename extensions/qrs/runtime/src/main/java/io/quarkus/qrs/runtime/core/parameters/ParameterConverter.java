package io.quarkus.qrs.runtime.core.parameters;

public interface ParameterConverter {

    Object convert(Object parameter);

}
