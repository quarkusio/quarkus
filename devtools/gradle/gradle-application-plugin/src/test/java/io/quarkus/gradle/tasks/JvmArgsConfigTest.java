package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test that verifies the Quarkus Gradle plugin properly configures JVM arguments and system property
 * for test execution, specifically:
 * - systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
 * - jvmArgs("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED")
 * - jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
 * - jvmArgs("--add-exports=java.base/jdk.internal.module=ALL-UNNAMED")
 */
public class JvmArgsConfigTest {

    private static final String LOGGING_MANAGER_PROP_NAME = "java.util.logging.manager";
    private static final String LOGGING_MANAGER_PROP_VALUE = "org.jboss.logmanager.LogManager";
    private static final String OPENS_LANG_INVOKE = "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED";
    private static final String OPENS_LANG = "--add-opens=java.base/java.lang=ALL-UNNAMED";
    private static final String EXPORTS_INTERNAL_MODULE = "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED";

    @TempDir
    Path testProjectDir;

    @Test
    void testJvmArgsAndSystemPropsAreConfigured() throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/jvmargs/main");
        assertThat(url).isNotNull();
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .build();

        BuildTask testResult = result.task(":test");
        assertThat(testResult).isNotNull();
        assertThat(testResult.getOutcome()).isEqualTo(SUCCESS);

        Path jvmConfigFile = testProjectDir.resolve("build/test-results/jvm-config.txt");
        assertThat(jvmConfigFile).exists();

        List<String> configLines = Files.readAllLines(jvmConfigFile);

        assertThat(configLines)
                .contains("%s=%s".formatted(LOGGING_MANAGER_PROP_NAME, LOGGING_MANAGER_PROP_VALUE));

        assertThat(configLines)
                .contains(OPENS_LANG_INVOKE)
                .contains(OPENS_LANG)
                .contains(EXPORTS_INTERNAL_MODULE);
    }
}
