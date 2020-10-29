package io.quarkus.rest.deployment.framework;

import io.quarkus.rest.common.deployment.framework.IndexedParameter;
import io.quarkus.rest.server.runtime.core.parameters.converters.ParameterConverterSupplier;

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
