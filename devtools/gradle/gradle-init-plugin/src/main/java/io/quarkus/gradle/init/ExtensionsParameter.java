package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class ExtensionsParameter implements BuildInitParameter<String> {

    static final ExtensionsParameter INSTANCE = new ExtensionsParameter();

    private ExtensionsParameter() {
    }

    @Override
    public String getName() {
        return "extensions";
    }

    @Override
    public String getParameterType() {
        return "";
    }
}
