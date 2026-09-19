package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class StringInitParameter implements BuildInitParameter<String> {

    static final StringInitParameter VERSION = new StringInitParameter("version");
    static final StringInitParameter DESCRIPTION = new StringInitParameter("description");
    static final StringInitParameter CLASS_NAME = new StringInitParameter("className");
    static final StringInitParameter PATH = new StringInitParameter("path");
    static final StringInitParameter QUARKUS_VERSION = new StringInitParameter("quarkusVersion");

    private final String name;

    private StringInitParameter(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParameterType() {
        return "";
    }
}
