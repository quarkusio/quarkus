package io.quarkus.gradle;

import static io.quarkus.gradle.LaunchUtils.dumpFileContentOnFailure;
import static io.quarkus.gradle.LaunchUtils.launch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.devmode.util.DevModeClient;

public class UberJarFormatWorksTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testUberJarFormatWorks() throws Exception {

        final File projectDir = getProjectDir("test-uber-jar-format-works");
        runGradleWrapper(projectDir, "clean", "build");
        final Path quarkusApp = projectDir.toPath().resolve("build");
        assertThat(quarkusApp).exists();
        Path jar = quarkusApp.resolve("uber-jar-test-1.0.0-SNAPSHOT-runner.jar");
        assertThat(jar).exists();

        File output = new File(projectDir, "build/output.log");
        output.createNewFile();
        DevModeClient devModeClient = new DevModeClient();

        Process process = launch(jar, output);
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES)
                        .until(() -> devModeClient.isCode("/hello", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThat(logs).contains("INFO").contains("cdi, resteasy");

            // test that the application name and version are properly set
            assertThat(devModeClient.getHttpResponse("/hello")).isEqualTo("hello");
        } finally {
            process.destroy();
        }
    }
}
