package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

@EnabledForJreRange(min = JRE.JAVA_25)
@DisabledOnOs(OS.WINDOWS)
public class JibAotTest extends QuarkusGradleWrapperTestBase {

    public static final String BASE_IMAGE_NAME = System.getProperty("user.name") + "/gradle-jib-aot:1.0.0-SNAPSHOT";
    public static final String AOT_IMAGE_NAME = BASE_IMAGE_NAME + "-aot";

    @Test
    public void shouldRunIntegrationTestAsPartOfBuild() throws Exception {
        File projectDir = getProjectDir("jib-aot");
        BuildResult buildResult = runGradleWrapper(projectDir, "build", "quarkusIntTest", "buildAotEnhancedImage");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":buildAotEnhancedImage"))).isTrue();

        Process process = new ProcessBuilder("docker", "images", "-q", AOT_IMAGE_NAME)
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        String imageId = new String(process.getInputStream().readAllBytes()).trim();
        assertFalse(imageId.isEmpty(), "Expected docker image " + AOT_IMAGE_NAME + " to exist");
    }

    @AfterEach
    void cleanUp() throws IOException, InterruptedException {
        // Remove the image after each test to keep Docker tidy
        new ProcessBuilder("docker", "rmi", "-f", AOT_IMAGE_NAME, BASE_IMAGE_NAME)
                .start()
                .waitFor();
    }
}
