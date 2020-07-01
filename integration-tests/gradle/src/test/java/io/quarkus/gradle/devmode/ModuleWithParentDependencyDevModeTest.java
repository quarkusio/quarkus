package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleWithParentDependencyDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "multi-module-parent-dependency";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hello");
    }
}
