package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomJarClassifierDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "custom-jar-classifier-dependency";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Hello from Quarkus REST");
    }
}
