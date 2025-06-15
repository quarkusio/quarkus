package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class AddEnvironmentVariablesDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "add-envitonment-variables-app";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("abcdef");
    }
}