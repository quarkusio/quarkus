package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Verifies that Quarkus logger output respects Gradle's testLogging.showStandardStreams = false
 * setting. When this setting is false, logger output (INFO, WARN, etc.) should not appear in
 * the Gradle build console output.
 *
 * This is a regression test for https://github.com/quarkusio/quarkus/issues/48763
 */
public class TestLoggingOutputCaptureTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testLoggerOutputRespectShowStandardStreams() throws Exception {
        File projectDir = getProjectDir("test-logging-output-capture");
        BuildResult buildResult = runGradleWrapper(projectDir, "test");

        // The test should pass
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":test"))).isTrue();

        String output = buildResult.getOutput();

        // Logger output should NOT appear in the build output because showStandardStreams = false
        assertThat(output).doesNotContain("MARKER_INFO_LOG_OUTPUT");
        assertThat(output).doesNotContain("MARKER_WARN_LOG_OUTPUT");
        assertThat(output).doesNotContain("MARKER_STDOUT_OUTPUT");
    }
}
