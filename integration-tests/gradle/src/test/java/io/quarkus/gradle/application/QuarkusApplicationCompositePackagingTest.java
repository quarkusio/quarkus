package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

class QuarkusApplicationCompositePackagingTest extends QuarkusApplicationGradleTestBase {

    @Test
    void includedLibraryIsPackagedAndExecutable() throws Exception {
        File projectDir = getProjectDir("application-plugin/composite-library");

        BuildResult result = runApplicationGradleWrapper(projectDir, "clean", "quarkusAppBuild");

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsKey(":quarkusAppBuild");

        Path includedJar = singleJar(projectDir.toPath().resolve("included-library/build/libs"), "included-library");
        assertJarContains(includedJar,
                "org/acme/library/IncludedGreeting.class",
                "included-library-marker.txt");

        Path packageDirectory = packageDirectory(projectDir);
        Path packagedLibrary = singleJar(packageDirectory.resolve("lib/main"), "included-library");
        assertJarContains(packagedLibrary,
                "org/acme/library/IncludedGreeting.class",
                "included-library-marker.txt");

        assertJarApplication(packageDirectory.resolve("quarkus-run.jar"),
                projectDir.toPath().resolve("build/composite-library-output.log"), Map.of(),
                "hello from the included library");
    }

    @Test
    void includedExtensionRuntimeAndDeploymentVariantsProduceExecutablePackage() throws Exception {
        File projectDir = getProjectDir("application-plugin/composite-extension");

        BuildResult result = runApplicationGradleWrapper(projectDir, "clean", "quarkusAppBuild");

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsKey(":quarkusAppBuild");

        Path runtimeJar = singleJar(
                projectDir.toPath().resolve("included-extension/runtime/build/libs"), "included-extension");
        assertJarContains(runtimeJar,
                "org/acme/extension/runtime/IncludedExtensionRuntime.class",
                "META-INF/quarkus-extension.properties");
        Path deploymentJar = singleJar(
                projectDir.toPath().resolve("included-extension/deployment/build/libs"),
                "included-extension-deployment");
        assertJarContains(deploymentJar,
                "org/acme/extension/deployment/IncludedExtensionProcessor.class");

        Path packageDirectory = packageDirectory(projectDir);
        Path packagedRuntime = singleJar(packageDirectory.resolve("lib/main"), "included-extension");
        assertJarContains(packagedRuntime,
                "org/acme/extension/runtime/IncludedExtensionRuntime.class",
                "META-INF/quarkus-extension.properties");
        assertThat(jarNames(packageDirectory.resolve("lib/main")))
                .noneMatch(name -> name.contains("included-extension-deployment"));

        Path output = projectDir.toPath().resolve("build/composite-extension-output.log");
        assertJarApplication(packageDirectory.resolve("quarkus-run.jar"), output, Map.of(),
                "included extension runtime / included extension deployment selected");
        assertThat(Files.readString(output, StandardCharsets.UTF_8)).contains("included-composite");
    }

    private static Path packageDirectory(File projectDir) {
        return projectDir.toPath().resolve(Path.of("build", "quarkus-builds", "app", "package"));
    }

    private static Path singleJar(Path directory, String nameFragment) throws IOException {
        List<Path> matches;
        try (Stream<Path> entries = Files.list(directory)) {
            matches = entries
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> path.getFileName().toString().contains(nameFragment))
                    .toList();
        }
        assertThat(matches).as("JARs matching '%s' in %s", nameFragment, directory).hasSize(1);
        return matches.get(0);
    }

    private static List<String> jarNames(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jar"))
                    .toList();
        }
    }

    private static void assertJarContains(Path archive, String... entries) throws IOException {
        assertThat(archive).isRegularFile();
        try (JarFile jar = new JarFile(archive.toFile())) {
            for (String entry : entries) {
                assertThat(jar.getJarEntry(entry)).as("%s in %s", entry, archive).isNotNull();
            }
        }
    }
}
