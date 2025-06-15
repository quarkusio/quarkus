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

public class MultiModuleUberJarTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testUberJarForMultiModule() throws Exception {

        final File projectDir = getProjectDir("uber-jar-for-multi-module-project");
        runGradleWrapper(projectDir, ":application:quarkusBuild");

        final Path applicationJar = projectDir.toPath().resolve("application").resolve("build");
        assertThat(applicationJar).exists();
        Path jar = applicationJar.resolve("application-unspecified-runner.jar");
        assertThat(jar).exists();

        File output = new File(projectDir, "application/build/output.log");
        output.createNewFile();
        DevModeClient devModeClient = new DevModeClient();

        Process process = launch(jar, output);
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await().pollDelay(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES)
                        .until(() -> devModeClient.isCode("/hello", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThat(logs).contains("INFO").contains("cdi, resteasy");
        } finally {
            process.destroy();
        }
    }
}
