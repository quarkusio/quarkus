package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class BooleanInitParameter implements BuildInitParameter<Boolean> {

    static final BooleanInitParameter NO_DOCKERFILES = new BooleanInitParameter("noDockerfiles");
    static final BooleanInitParameter NO_BUILD_TOOL_WRAPPER = new BooleanInitParameter("noBuildToolWrapper");
    static final BooleanInitParameter NO_CODE = new BooleanInitParameter("noCode");

    private final String name;

    private BooleanInitParameter(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean getParameterType() {
        return Boolean.FALSE;
    }
}
