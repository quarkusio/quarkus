package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomWorkingDirDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "custom-working-dir-app";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("from-custom-file");
    }
}
