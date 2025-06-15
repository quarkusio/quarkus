package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

@org.junit.jupiter.api.Tag("failsOnJDK20")
public class MultiSourceProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "multi-source-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello")).contains("hello from JavaComponent");
        replace("src/main/java/org/acme/service/SimpleService.java",
                ImmutableMap.of("return \"hello from JavaComponent\";", "return \"hi\";"));

        assertUpdatedResponseContains("/hello", "hi");

        replace("src/main/kotlin/org/acme/ExampleResource.kt", ImmutableMap.of("fun hello(): String = service.hello()",
                "fun hello(): String = service.hello() + \"!\""));

        assertUpdatedResponseContains("/hello", "hi!");
    }
}
