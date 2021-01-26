package org.jboss.resteasy.reactive.server.processor;

import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ServerIndexedParameter extends IndexedParameter<ServerIndexedParameter> {
    protected ParameterConverterSupplier converter;
    private ParameterExtractor customerParameterExtractor;

    public ParameterConverterSupplier getConverter() {
        return converter;
    }

    public ServerIndexedParameter setConverter(ParameterConverterSupplier converter) {
        this.converter = converter;
        return this;
    }

    public ParameterExtractor getCustomerParameterExtractor() {
        return customerParameterExtractor;
    }

    public ServerIndexedParameter setCustomerParameterExtractor(ParameterExtractor customerParameterExtractor) {
        this.customerParameterExtractor = customerParameterExtractor;
        return this;
    }
}
