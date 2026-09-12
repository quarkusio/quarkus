package io.quarkus.gradle.init;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuarkusAppInitSpecTest {

    @Test
    void typeIsQuarkusApp() {
        QuarkusAppInitSpec spec = new QuarkusAppInitSpec();

        assertThat(spec.getType()).isEqualTo("quarkus-app");
    }

    @Test
    void declaresAllSupportedParameters() {
        QuarkusAppInitSpec spec = new QuarkusAppInitSpec();

        assertThat(spec.getParameters()).containsExactly(ProjectNameParameter.INSTANCE, GroupIdParameter.INSTANCE,
                ExtensionsParameter.INSTANCE, DslParameter.INSTANCE, StringInitParameter.VERSION,
                StringInitParameter.DESCRIPTION, StringInitParameter.CLASS_NAME, StringInitParameter.PATH,
                StringInitParameter.QUARKUS_VERSION, BooleanInitParameter.NO_DOCKERFILES,
                BooleanInitParameter.NO_BUILD_TOOL_WRAPPER, BooleanInitParameter.NO_CODE);
    }
}
