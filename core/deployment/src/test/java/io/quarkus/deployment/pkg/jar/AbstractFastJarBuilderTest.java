package io.quarkus.deployment.pkg.jar;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AbstractFastJarBuilderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagesAllDirectoryRootsIntoOneDependencyJar() throws Exception {
        Path classes = Files.createDirectories(temporaryDirectory.resolve("classes"));
        Path resources = Files.createDirectories(temporaryDirectory.resolve("resources"));
        Files.createDirectories(classes.resolve("org/acme"));
        Files.writeString(classes.resolve("org/acme/Greeting.class"), "class");
        Files.createDirectories(resources.resolve("META-INF"));
        Files.writeString(resources.resolve("META-INF/application.properties"), "greeting=hello");

        Path dependencyJar = temporaryDirectory.resolve("lib/dependency.jar");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            AbstractFastJarBuilder.packageClasses(List.of(classes, resources), dependencyJar, true, null, executor);
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        try (ZipFile archive = new ZipFile(dependencyJar.toFile())) {
            assertThat(archive.getEntry("org/acme/Greeting.class")).isNotNull();
            assertThat(archive.getEntry("META-INF/application.properties")).isNotNull();
        }
    }
}
