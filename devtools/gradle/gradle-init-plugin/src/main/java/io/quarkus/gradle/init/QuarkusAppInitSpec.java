package io.quarkus.gradle.init;

import java.util.List;

import org.gradle.buildinit.specs.BuildInitParameter;
import org.gradle.buildinit.specs.BuildInitSpec;

class QuarkusAppInitSpec implements BuildInitSpec {

    @Override
    public String getType() {
        return "quarkus-app";
    }

    @Override
    public List<BuildInitParameter<?>> getParameters() {
        return List.of(ProjectNameParameter.INSTANCE, GroupIdParameter.INSTANCE, ExtensionsParameter.INSTANCE,
                DslParameter.INSTANCE, StringInitParameter.VERSION, StringInitParameter.DESCRIPTION,
                StringInitParameter.CLASS_NAME, StringInitParameter.PATH, StringInitParameter.QUARKUS_VERSION,
                BooleanInitParameter.NO_DOCKERFILES, BooleanInitParameter.NO_BUILD_TOOL_WRAPPER,
                BooleanInitParameter.NO_CODE);
    }
}
