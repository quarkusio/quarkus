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

import io.quarkus.test.devmode.util.DevModeTestUtils;

public class LegacyJarFormatWorksTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testLegacyJarFormatWorks() throws Exception {

        final File projectDir = getProjectDir("test-that-legacy-jar-format-works");

        runGradleWrapper(projectDir, "clean", "build");

        final Path runnerJar = projectDir.toPath().resolve("build")
                .resolve("code-with-quarkus-1.0.0-SNAPSHOT-runner.jar");
        assertThat(runnerJar).exists();

        File output = new File(projectDir, "build/output.log");
        output.createNewFile();
        Process process = launch(runnerJar, output);
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

            assertThat(logs).contains("INFO").contains("cdi, resteasy");

            // test that the application name and version are properly set
            assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("hello");
        } finally {
            process.destroy();
        }
    }
}
