package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationProcessorMultiModuleDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "annotation-processor-multi-module";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("seatNumber");
    }
}
