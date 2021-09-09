package org.jboss.resteasy.reactive.server.model;

import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ServerMethodParameter extends MethodParameter {

    public ParameterConverterSupplier converter;
    public ParameterExtractor customerParameterExtractor;

    public ServerMethodParameter() {
    }

    public ServerMethodParameter(String name, String type, String declaredType, String declaredUnresolvedType,
            ParameterType parameterType, boolean single,
            String signature,
            ParameterConverterSupplier converter, String defaultValue, boolean isObtainedAsCollection, boolean isOptional,
            boolean encoded,
            ParameterExtractor customerParameterExtractor) {
        super(name, type, declaredType, declaredUnresolvedType, signature, parameterType, single, defaultValue,
                isObtainedAsCollection, isOptional,
                encoded);
        this.converter = converter;
        this.customerParameterExtractor = customerParameterExtractor;
    }
}
