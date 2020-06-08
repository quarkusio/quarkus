package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class InjectQuarkusAppPropertiesDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "inject-quarkus-app-properties";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "-s" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello")).contains("code-with-quarkus 1.0.0-SNAPSHOT");
    }
}
