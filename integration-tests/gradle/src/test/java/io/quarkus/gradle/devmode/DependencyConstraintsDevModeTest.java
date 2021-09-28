package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyConstraintsDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "dependency-constraints-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hello");
    }
}
