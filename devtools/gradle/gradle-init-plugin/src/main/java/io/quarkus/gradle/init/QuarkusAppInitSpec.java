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
        return List.of(StringInitParameter.PROJECT_NAME, StringInitParameter.GROUP_ID, StringInitParameter.EXTENSIONS,
                StringInitParameter.DSL, StringInitParameter.VERSION, StringInitParameter.DESCRIPTION,
                StringInitParameter.CLASS_NAME, StringInitParameter.PATH, StringInitParameter.QUARKUS_VERSION,
                BooleanInitParameter.NO_DOCKERFILES, BooleanInitParameter.NO_CODE);
    }
}
