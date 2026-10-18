package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class CustomFileSystemProviderTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("custom-filesystem-provider");

        BuildResult build = runGradleWrapper(projectDir, "clean", ":application:test");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":application:test"))).isTrue();
    }
}
