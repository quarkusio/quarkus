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

import io.quarkus.runtime.util.HashUtil;

@ExtendWith(SoftAssertionsExtension.class)
public class ImplementationFilesQuarkusBuildTest extends QuarkusGradleWrapperTestBase {
    @InjectSoftAssertions
    SoftAssertions soft;

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("implementation-files");

        soft.assertThat(runGradleWrapper(projectDir, "clean", ":common:build", "--no-build-cache").unsuccessfulTasks())
                .isEmpty();
        Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        soft.assertThat(commonLibs).isDirectory();
        Path commonJar = commonLibs.resolve("common.jar");
        soft.assertThat(commonJar).isNotEmptyFile();

        soft.assertThat(runGradleWrapper(projectDir, ":application-files:build", "--no-build-cache").unsuccessfulTasks())
                .isEmpty();
        Path applicationLib = projectDir.toPath().resolve("application-files").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        soft.assertThat(applicationLib).isDirectory();
        String commonFileName = HashUtil.sha1(commonJar.getParent().toString()) + "." + commonJar.getFileName();
        soft.assertThat(applicationLib.resolve(commonFileName)).isNotEmptyFile();

        soft.assertThat(runGradleWrapper(projectDir, ":application-dep:build", "--no-build-cache").unsuccessfulTasks())
                .isEmpty();
        applicationLib = projectDir.toPath().resolve("application-dep").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        soft.assertThat(applicationLib).isDirectory();
        commonFileName = "quarkus-basic-multi-module-build." + commonJar.getFileName();
        soft.assertThat(applicationLib.resolve(commonFileName)).isNotEmptyFile();

        try {
            soft.assertAll();
        } catch (AssertionError ex) {
            try (Stream<Path> files = Files.walk(projectDir.toPath())) {
                files.map(Path::toString).sorted().forEach(System.err::println);
            }
            throw ex;
        }
    }
}
