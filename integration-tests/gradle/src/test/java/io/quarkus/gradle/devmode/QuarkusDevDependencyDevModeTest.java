package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarkusDevDependencyDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "quarkus-dev-dependency";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Quarkus");
    }
}
