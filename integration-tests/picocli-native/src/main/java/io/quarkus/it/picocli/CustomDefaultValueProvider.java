package io.quarkus.it.picocli;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;

public class CustomDefaultValueProvider implements IDefaultValueProvider {

    @Override
    public String defaultValue(ArgSpec argSpec) throws Exception {
        return "default-value";
    }
}
