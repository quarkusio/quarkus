package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_GENERATE_CODE_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.gradle.testing.BaseGradleTest;

public class EagerResolutionTaskTest extends BaseGradleTest {

    private static Stream<String> tasksToTest() {
        return Stream.of(
                QUARKUS_GENERATE_CODE_TASK_NAME,
                QUARKUS_BUILD_TASK_NAME,
                "build",
                "classes");
    }

    @ParameterizedTest
    @MethodSource("tasksToTest")
    public void eagerResolutionConfigurationBuildsSuccessfully(String taskName) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/crypto/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        File projectDir = new File(testProjectDir + "/build.gradle.kts");

        String addingQuarkusExtension = """
                quarkus {
                  cachingRelevantProperties.add("FOO_ENV_VAR")
                  quarkusBuildProperties.put("quarkus.package.jar.type", "fast-jar")
                  quarkusBuildProperties.putAll(
                    provider {
                      tasks
                        .named("jar", Jar::class.java)
                        .get()
                        .manifest
                        .attributes
                        .map { e -> "quarkus.package.jar.manifest.attributes.\\"${e.key}\\"" to e.value.toString() }
                        .toMap()
                    }
                  )
                }

                """;
        try {
            Files.write(projectDir.toPath(), addingQuarkusExtension.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BuildResult firstBuild = buildResult(Map.of(), taskName);
        // the build cache is enabled for these builds, so an unchanged project can legitimately serve
        // the task from the cache instead of executing it; either outcome means it did not fail
        assertThat(firstBuild.task(":quarkusGenerateCode").getOutcome()).isIn(SUCCESS, FROM_CACHE);
    }
}
