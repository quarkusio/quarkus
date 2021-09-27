package org.jboss.resteasy.reactive.server.model;

import java.util.Objects;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ServerMethodParameter extends MethodParameter {

    public ParameterConverterSupplier converter;
    public ParameterExtractor customParameterExtractor;

    public ServerMethodParameter() {
    }

    public ServerMethodParameter(String name, String type, String declaredType, String declaredUnresolvedType,
            ParameterType parameterType, boolean single,
            String signature,
            ParameterConverterSupplier converter, String defaultValue, boolean obtainedAsCollection, boolean optional,
            boolean encoded,
            ParameterExtractor customParameterExtractor) {
        super(name, type, declaredType, declaredUnresolvedType, signature, parameterType, single, defaultValue,
                obtainedAsCollection, optional,
                encoded);
        this.converter = converter;
        this.customParameterExtractor = customParameterExtractor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServerMethodParameter that = (ServerMethodParameter) o;
        if (!super.equals(that)) {
            return false;
        }
        return Objects.equals(converter, that.converter)
                && Objects.equals(customParameterExtractor, that.customParameterExtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(converter, customParameterExtractor) + super.hashCode();
    }
}
