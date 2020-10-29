package io.quarkus.rest.server.runtime.model;

import io.quarkus.rest.common.runtime.model.MethodParameter;
import io.quarkus.rest.common.runtime.model.ParameterType;
import io.quarkus.rest.server.runtime.core.parameters.converters.ParameterConverterSupplier;

public class ServerMethodParameter extends MethodParameter {

    public ParameterConverterSupplier converter;

    public ServerMethodParameter() {
    }

    public ServerMethodParameter(String name, String type, String declaredType, ParameterType parameterType, boolean single,
            ParameterConverterSupplier converter, String defaultValue, boolean isObtainedAsCollection, boolean encoded) {
        super(name, type, declaredType, parameterType, single, defaultValue, isObtainedAsCollection, encoded);
        this.converter = converter;
    }
}
