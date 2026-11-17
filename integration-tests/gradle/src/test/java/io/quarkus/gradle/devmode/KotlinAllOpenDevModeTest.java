package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;

/**
 * Classes recompiled in dev mode must be compiled with the Kotlin compiler plugins configured for the project.
 * Without the all-open plugin the intercepted {@code suspend} function would be final after the reload.
 */
public class KotlinAllOpenDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "kotlin-all-open-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).isEqualTo("intercepted:hello");

        final String uuid = UUID.randomUUID().toString();
        replace("src/main/kotlin/org/acme/GreetingService.kt",
                ImmutableMap.of("\"hello\"", "\"" + uuid + "\""));

        assertUpdatedResponseContains("/hello", uuid);
        assertThat(getHttpResponse("/hello")).isEqualTo("intercepted:" + uuid);
    }
}
