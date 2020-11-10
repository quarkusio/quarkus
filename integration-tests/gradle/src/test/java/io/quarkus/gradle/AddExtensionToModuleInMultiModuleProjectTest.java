package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class AddExtensionToModuleInMultiModuleProjectTest extends QuarkusGradleWrapperTestBase {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("add-extension-multi-module");

        runGradleWrapper(projectDir, ":application:addExtension", "--extensions=openshift");

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("settings.gradle");
        assertThat(applicationLib).doesNotExist();

        final Path appBuild = projectDir.toPath().resolve("application").resolve("build.gradle");
        assertThat(appBuild).exists();
        assertThat(readFile(appBuild)).contains("implementation 'io.quarkus:quarkus-openshift'");

        runGradleWrapper(projectDir, ":application:removeExtension", "--extensions=openshift");
        assertThat(readFile(appBuild)).doesNotContain("implementation 'io.quarkus:quarkus-openshift'");
    }

    private static String readFile(Path file) throws IOException {
        final char[] charBuffer = new char[DEFAULT_BUFFER_SIZE];
        int n = 0;
        final StringWriter output = new StringWriter();
        try (BufferedReader input = Files.newBufferedReader(file)) {
            while ((n = input.read(charBuffer)) != -1) {
                output.write(charBuffer, 0, n);
            }
        }
        return output.getBuffer().toString();
    }
}
