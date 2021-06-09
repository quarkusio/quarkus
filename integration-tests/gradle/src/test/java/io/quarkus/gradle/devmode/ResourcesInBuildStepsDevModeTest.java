package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import io.quarkus.runtime.LaunchMode;

public class ResourcesInBuildStepsDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "test-resources-in-build-steps";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev", "-s" };
    }

    @Override
    protected void beforeQuarkusDev() throws Exception {
        runGradleWrapper(projectDir, ":application:publishAcmeExt");
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse()).contains("homepage");

        assertThat(getHttpResponse("/hello")).contains("hello");

        final File projectDir = getProjectDir();
        final Path buildDir = projectDir.toPath().resolve("application").resolve("build");
        final Path prodResourcesTxt = buildDir.resolve(LaunchMode.DEVELOPMENT + "-resources.txt");
        assertThat(prodResourcesTxt).hasContent("main");
    }
}
