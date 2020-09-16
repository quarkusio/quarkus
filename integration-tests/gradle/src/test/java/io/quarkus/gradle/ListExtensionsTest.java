package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ListExtensionsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testListExtensionsWork() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("list-extension-single-module");
        runGradleWrapper(projectDir, ":listExtension");

        List<String> outputLogLines = listExtensions(projectDir, ":listExtension");

        assertThat(outputLogLines).anyMatch(line -> line.contains("quarkus-resteasy"));
        assertThat(outputLogLines).anyMatch(line -> line.contains("quarkus-vertx"));
    }

    private List<String> listExtensions(File projectDir, String... args) throws IOException, InterruptedException {

        File outputLog = new File(projectDir, "command-output.log");

        return Files.readAllLines(outputLog.toPath());
    }

}