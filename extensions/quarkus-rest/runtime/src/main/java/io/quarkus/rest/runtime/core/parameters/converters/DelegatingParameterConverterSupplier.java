package io.quarkus.rest.runtime.core.parameters.converters;

public interface DelegatingParameterConverterSupplier extends ParameterConverterSupplier {
    public ParameterConverterSupplier getDelegate();
}
