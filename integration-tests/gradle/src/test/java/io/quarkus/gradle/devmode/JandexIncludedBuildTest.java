package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class JandexIncludedBuildTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "jandex-included-build-kordamp";
    }

    /**
     * Test that jandex works correctly across includedBuild in dev mode
     */
    @Override
    protected void testDevMode() {
        assertThat(getHttpResponse("/hello")).contains("hello included-kordamp");
        replace("acme-subproject-nested/acme-nested/src/main/java/org/acme/nested/SimpleService.java",
                ImmutableMap.of("return \"included-kordamp\";", "return \"included-kordamp-modified\";"));
        assertUpdatedResponseContains("/hello", "hello included-kordamp-modified");
    }
}
