package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class DotEnvQuarkusDevModeConfigurationTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "dotenv-config-java-module";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hey");
    }
}
