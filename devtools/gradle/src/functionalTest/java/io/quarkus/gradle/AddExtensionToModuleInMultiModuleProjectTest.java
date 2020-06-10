package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.util.IoUtils;

import static org.assertj.core.api.Assertions.assertThat;


public class AddExtensionToModuleInMultiModuleProjectTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("add-extension-multi-module");
        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments(":application:addExtension", "--extensions=hibernate-orm"))
                .withProjectDir(projectDir)
                .build();
        
        final Path applicationLib = projectDir.toPath().resolve("application").resolve("settings.gradle");
        assertThat(applicationLib).doesNotExist();
        
        final Path appBuild = projectDir.toPath().resolve("application").resolve("build.gradle");
        assertThat(appBuild).exists();
        assertThat(IoUtils.readFile(appBuild)).contains("implementation 'io.quarkus:quarkus-hibernate-orm'");
    }
}
