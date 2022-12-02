package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationProcessorSimpleModuleDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "annotation-processor-simple-module";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("seatNumber");
    }
}
