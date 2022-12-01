package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.HashUtil;

public class ImplementationFilesQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("implementation-files");

        runGradleWrapper(projectDir, ":common:build");
        final Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        final Path commonJar = commonLibs.resolve("common.jar");
        assertThat(commonJar).exists();

        runGradleWrapper(projectDir, ":application:build");
        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        assertThat(applicationLib).exists();
        String commonFileName = HashUtil.sha1(commonJar.getParent().toString()) + "." + commonJar.getFileName();
        assertThat(applicationLib.resolve(commonFileName)).exists();
    }
}
