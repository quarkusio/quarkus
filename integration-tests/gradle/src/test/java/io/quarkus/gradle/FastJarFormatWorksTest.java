package io.quarkus.gradle;

import static io.quarkus.gradle.LaunchUtils.dumpFileContentOnFailure;
import static io.quarkus.gradle.LaunchUtils.launch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.devmode.util.DevModeTestUtils;

public class FastJarFormatWorksTest extends QuarkusGradleWrapperTestBase {
    private static Future<?> jarRun;

    @Test
    public void testFastJarFormatWorks() throws Exception {

        final File projectDir = getProjectDir("test-that-fast-jar-format-works");

        runGradleWrapper(projectDir, "clean", "build");

        final Path quarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-app");
        assertThat(quarkusApp).exists();
        Path jar = quarkusApp.resolve("quarkus-run.jar");
        assertThat(jar).exists();

        File output = new File(projectDir, "build/output.log");
        output.createNewFile();
        Process process = launch(jar, output);
        try {
            //Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES)
                        .until(() -> DevModeTestUtils.isCode("/hello", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertThat(DevModeTestUtils.getHttpResponse("/hello", () -> {
                return jarRun == null ? null : jarRun.isDone() ? "jar run mode has terminated" : null;
            }).equals("hello"));
        } finally {
            process.destroy();
        }
    }

    private void assertThatOutputWorksCorrectly(String logs) {
        assertThat(logs.isEmpty()).isFalse();
        String infoLogLevel = "INFO";
        assertThat(logs.contains(infoLogLevel)).isTrue();
        assertThat(logs.contains("cdi, resteasy")).isTrue();
    }

}
