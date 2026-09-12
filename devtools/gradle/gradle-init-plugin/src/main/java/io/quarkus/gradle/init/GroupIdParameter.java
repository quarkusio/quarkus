package io.quarkus.gradle.init;

import org.gradle.buildinit.specs.BuildInitParameter;

final class GroupIdParameter implements BuildInitParameter<String> {

    static final GroupIdParameter INSTANCE = new GroupIdParameter();

    private GroupIdParameter() {
    }

    @Override
    public String getName() {
        return "groupId";
    }

    @Override
    public String getParameterType() {
        return "";
    }
}
