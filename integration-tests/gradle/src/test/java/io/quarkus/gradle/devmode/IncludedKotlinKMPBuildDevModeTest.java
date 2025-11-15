package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

public class IncludedKotlinKMPBuildDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "included-kotlin-kmp-build";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello/kmp")).contains("hi from KMP");
        assertThat(getHttpResponse("/hello/jvm")).contains("hi from JVM-only sources");
    }
}
