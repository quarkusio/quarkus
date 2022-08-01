package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.util.function.Supplier;

public interface ParameterConverterSupplier extends Supplier<ParameterConverter> {
    String getClassName();

}
