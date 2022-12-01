package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class MultiModuleNamedInjectionDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "multi-module-named-injection";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "-s" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello")).contains("hello true true");

        replace("quarkus/src/main/java/org/acme/app/ExampleResource.java",
                ImmutableMap.of("return \"hello \"", "return \"modified \""));

        assertUpdatedResponseContains("/hello", "modified true true");
    }
}
