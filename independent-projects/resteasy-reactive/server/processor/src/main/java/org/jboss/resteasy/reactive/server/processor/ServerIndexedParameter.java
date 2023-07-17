package org.jboss.resteasy.reactive.server.processor;

import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ServerIndexedParameter extends IndexedParameter<ServerIndexedParameter> {
    protected ParameterConverterSupplier converter;
    private ParameterExtractor customParameterExtractor;

    public ParameterConverterSupplier getConverter() {
        return converter;
    }

    public ServerIndexedParameter setConverter(ParameterConverterSupplier converter) {
        this.converter = converter;
        return this;
    }

    public ParameterExtractor getCustomParameterExtractor() {
        return customParameterExtractor;
    }

    public ServerIndexedParameter setCustomParameterExtractor(ParameterExtractor customerParameterExtractor) {
        this.customParameterExtractor = customerParameterExtractor;
        return this;
    }
}
