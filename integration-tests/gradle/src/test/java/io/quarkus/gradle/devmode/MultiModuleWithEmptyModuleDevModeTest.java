package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiModuleWithEmptyModuleDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "multi-module-with-empty-module";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":modB:quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("foo bar");
    }
}
