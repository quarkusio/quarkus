package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomQuarkusDevModeConfigurationTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "custom-config-java-module";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/working-dir")).contains("build");
    }
}
