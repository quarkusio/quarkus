package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class QuarkusComponentTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBuild() throws Exception {
        final File projectDir = getProjectDir("quarkus-component-test");

        BuildResult build = runGradleWrapper(projectDir, "test");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":test"))).isTrue();
    }
}
