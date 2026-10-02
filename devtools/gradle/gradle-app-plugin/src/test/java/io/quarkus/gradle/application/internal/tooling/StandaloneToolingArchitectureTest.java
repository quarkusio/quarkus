package io.quarkus.gradle.application.internal.tooling;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

class StandaloneToolingArchitectureTest {

    private static final String TOOLING_PACKAGE_PATH = "io/quarkus/gradle/application/internal/tooling/";

    private static final Map<String, String> FORBIDDEN_REFERENCES = Map.of(
            "io/quarkus/gradle/tooling/", "legacy tooling package",
            "io/quarkus/gradle/dependency/ApplicationDeploymentClasspathBuilder",
            "legacy application deployment classpath builder",
            "io/quarkus/gradle/dependency/QuarkusComponentVariants", "legacy component variants",
            "io/quarkus/gradle/tasks/QuarkusApplicationModelTask", "legacy application-model task",
            "io/quarkus/gradle/QuarkusPlugin", "legacy plugin lifecycle");

    @Test
    void standaloneToolingPackageDoesNotReferenceLegacyImplementation() throws IOException, URISyntaxException {
        List<ClassBytes> toolingClasses = compiledToolingClasses();
        List<String> violations = new ArrayList<>();

        for (ClassBytes toolingClass : toolingClasses) {
            String bytecode = new String(toolingClass.bytes(), StandardCharsets.ISO_8859_1);
            FORBIDDEN_REFERENCES.forEach((reference, description) -> {
                if (bytecode.contains(reference)) {
                    violations.add(toolingClass.name() + " references " + description + " (" + reference + ")");
                }
            });
        }

        assertThat(toolingClasses).as("compiled standalone tooling classes").isNotEmpty();
        assertThat(violations)
                .as("standalone tooling must remain isolated from the legacy Gradle provider")
                .isEmpty();
    }

    private static List<ClassBytes> compiledToolingClasses() throws IOException, URISyntaxException {
        Path productionLocation = Path.of(GradleApplicationModelBuilder.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        if (Files.isDirectory(productionLocation)) {
            Path packageDirectory = productionLocation.resolve(TOOLING_PACKAGE_PATH);
            try (var paths = Files.walk(packageDirectory)) {
                return paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .sorted()
                        .map(path -> readClass(productionLocation, path))
                        .toList();
            }
        }

        try (JarFile jar = new JarFile(productionLocation.toFile())) {
            return jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(TOOLING_PACKAGE_PATH))
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .sorted((left, right) -> left.getName().compareTo(right.getName()))
                    .map(entry -> readClass(jar, entry.getName()))
                    .toList();
        }
    }

    private static ClassBytes readClass(Path root, Path path) {
        try {
            return new ClassBytes(root.relativize(path).toString(), Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static ClassBytes readClass(JarFile jar, String entryName) {
        try (InputStream input = jar.getInputStream(jar.getJarEntry(entryName))) {
            return new ClassBytes(entryName, input.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + entryName + " from " + jar.getName(), e);
        }
    }

    private record ClassBytes(String name, byte[] bytes) {
    }
}
