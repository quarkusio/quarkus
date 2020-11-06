package org.jboss.resteasy.reactive.server.processor;

import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ServerIndexedParameter extends IndexedParameter<ServerIndexedParameter> {
    protected ParameterConverterSupplier converter;

    public ParameterConverterSupplier getConverter() {
        return converter;
    }

    public ServerIndexedParameter setConverter(ParameterConverterSupplier converter) {
        this.converter = converter;
        return this;
    }
}
