package io.quarkus.azure.functions.runtime;

import com.microsoft.azure.functions.spi.inject.FunctionInstanceInjector;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.Quarkus;

public class QuarkusAzureFunctionsInjector implements FunctionInstanceInjector {
    public QuarkusAzureFunctionsInjector() {
        Quarkus.manualInitialize();
        Quarkus.manualStart();
    }

    @Override
    public <T> T getInstance(Class<T> aClass) throws Exception {
        return Arc.container().instance(aClass).get();
    }
}
