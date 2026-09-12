package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class ProjectNameParameter implements BuildInitParameter<String> {

    static final ProjectNameParameter INSTANCE = new ProjectNameParameter();

    private ProjectNameParameter() {
    }

    @Override
    public String getName() {
        return "projectName";
    }

    @Override
    public String getParameterType() {
        return "";
    }
}
