package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;

public class BasicKotlinApplicationModuleDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "basic-kotlin-application-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hello");

        final String uuid = UUID.randomUUID().toString();
        replace("src/main/kotlin/org/acme/GreetingResource.kt",
                ImmutableMap.of("return \"hello\"", "return \"" + uuid + "\""));

        assertUpdatedResponseContains("/hello", uuid);
    }
}
