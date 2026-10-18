package org.jboss.resteasy.reactive.server.core.parameters.converters;

public interface DelegatingParameterConverterSupplier extends ParameterConverterSupplier {
    public ParameterConverterSupplier getDelegate();
}
