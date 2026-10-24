package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class DslParameter implements BuildInitParameter<String> {

    static final DslParameter INSTANCE = new DslParameter();

    private DslParameter() {
    }

    @Override
    public String getName() {
        return "dsl";
    }

    @Override
    public String getParameterType() {
        return "";
    }
}
