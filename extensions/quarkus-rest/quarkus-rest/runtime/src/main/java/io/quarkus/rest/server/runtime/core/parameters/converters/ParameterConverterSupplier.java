package io.quarkus.rest.server.runtime.core.parameters.converters;

import java.util.function.Supplier;

public interface ParameterConverterSupplier extends Supplier<ParameterConverter> {
    String getClassName();

}
