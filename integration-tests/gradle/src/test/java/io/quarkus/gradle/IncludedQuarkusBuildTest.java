package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/pull/38607">{@code IllegalStateException} when
 * Quarkus project is in an <em>included</em> Gradle build</a>.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class IncludedQuarkusBuildTest extends QuarkusGradleWrapperTestBase {
    @InjectSoftAssertions
    SoftAssertions soft;

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("included-build");

        Path prjDir = projectDir.toPath();
        Path target1 = prjDir.resolve("included").resolve("gradle.properties");
        Path target2 = prjDir.resolve("included").resolve("nested").resolve("gradle.properties");
        Files.deleteIfExists(target1);
        Files.deleteIfExists(target2);
        Files.copy(prjDir.resolve("gradle.properties"), target1);
        Files.copy(prjDir.resolve("gradle.properties"), target2);

        soft.assertThat(runGradleWrapper(projectDir, "clean", "jar", "--no-build-cache").unsuccessfulTasks())
                .isEmpty();

        try {
            soft.assertAll();
        } catch (AssertionError ex) {
            try (Stream<Path> files = Files.walk(prjDir)) {
                files.map(Path::toString).sorted().forEach(System.err::println);
            }
            throw ex;
        }
    }
}
