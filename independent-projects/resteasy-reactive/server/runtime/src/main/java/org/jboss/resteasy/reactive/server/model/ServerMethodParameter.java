package org.jboss.resteasy.reactive.server.model;

import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

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
