package io.quarkus.rest.server.runtime.core.parameters.converters;

public interface DelegatingParameterConverterSupplier extends ParameterConverterSupplier {
    public ParameterConverterSupplier getDelegate();
}
