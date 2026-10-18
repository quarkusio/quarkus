package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

// Reproduces https://github.com/quarkusio/quarkus/issues/48159
public class TestWithSidecarModule extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {
        final File projectDir = getProjectDir("test-with-sidecar-module");
        final BuildResult build = runGradleWrapper(projectDir, "clean", ":my-module:test");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":my-module:test"))).isTrue();
    }
}
