package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;

public class BasicJavaLibraryModuleDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "basic-java-library-module";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev", "-s" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse())
                .contains("ready")
                .contains("application")
                .contains("org.acme")
                .contains("1.0.0-SNAPSHOT");

        assertThat(getHttpResponse("/hello")).contains("hello");

        final String uuid = UUID.randomUUID().toString();
        replace("application/src/main/java/org/acme/ExampleResource.java",
                ImmutableMap.of("return \"hello\";", "return \"" + uuid + "\";"));

        assertUpdatedResponseContains("/hello", uuid);
    }
}
