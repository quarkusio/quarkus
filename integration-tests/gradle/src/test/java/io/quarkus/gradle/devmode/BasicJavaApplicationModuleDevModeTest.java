package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.google.common.collect.ImmutableMap;

@DisabledOnOs(OS.WINDOWS)
public class BasicJavaApplicationModuleDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "basic-java-application-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "run", "-s" };
    }

    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hello");

        final String uuid = UUID.randomUUID().toString();
        replace("src/main/java/org/acme/ExampleResource.java",
                ImmutableMap.of("return \"hello\";", "return \"" + uuid + "\";"));

        assertUpdatedResponseContains("/hello", uuid);
    }
}
