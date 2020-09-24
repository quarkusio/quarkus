package io.quarkus.rest.runtime.core.parameters.converters;

import java.util.function.Supplier;

public interface ParameterConverterSupplier extends Supplier<ParameterConverter> {
    public String getClassName();
}
