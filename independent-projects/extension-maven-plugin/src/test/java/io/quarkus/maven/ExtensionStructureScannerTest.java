package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.structure.FooFeature;
import io.quarkus.maven.structure.FooProcessor;
import io.quarkus.maven.structure.FooRecorder;
import io.quarkus.platform.tools.ExtensionStructure.ClassFacts;

class ExtensionStructureScannerTest {

    private static final List<String> FIXTURES = List.of(
            "io/quarkus/maven/structure/FooProcessor.class",
            "io/quarkus/maven/structure/FooProcessor$Nested.class",
            "io/quarkus/maven/structure/FooSubstitution.class",
            "io/quarkus/maven/structure/FooFeature.class",
            "io/quarkus/maven/structure/FooRecorder.class");

    @TempDir
    Path tmp;

    @Test
    void scansTopLevelClassesOfADirectory() throws IOException {
        for (String fixture : FIXTURES) {
            Path target = tmp.resolve(fixture);
            Files.createDirectories(target.getParent());
            try (InputStream in = fixture(fixture)) {
                Files.copy(in, target);
            }
        }

        assertThat(ExtensionStructureScanner.scan(tmp)).containsExactlyInAnyOrderElementsOf(expectedFacts());
    }

    @Test
    void scansTopLevelClassesOfAJar() throws IOException {
        Path jar = tmp.resolve("foo.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String fixture : FIXTURES) {
                out.putNextEntry(new JarEntry(fixture));
                try (InputStream in = fixture(fixture)) {
                    in.transferTo(out);
                }
                out.closeEntry();
            }
        }

        assertThat(ExtensionStructureScanner.scan(jar)).containsExactlyInAnyOrderElementsOf(expectedFacts());
    }

    @Test
    void readsTheDeploymentArtifactOfAnExtension() throws IOException {
        Path descriptor = tmp.resolve("META-INF/quarkus-extension.properties");
        Files.createDirectories(descriptor.getParent());
        try (OutputStream out = Files.newOutputStream(descriptor)) {
            out.write("deployment-artifact=io.quarkus\\:quarkus-foo-deployment\\:999-SNAPSHOT\n".getBytes());
        }

        assertThat(ExtensionStructureScanner.deploymentArtifactOf(tmp))
                .hasValue("io.quarkus:quarkus-foo-deployment");
    }

    @Test
    void findsNoDeploymentArtifactInALibrary() throws IOException {
        assertThat(ExtensionStructureScanner.deploymentArtifactOf(tmp)).isEmpty();
    }

    private static List<ClassFacts> expectedFacts() {
        return List.of(
                new ClassFacts(FooProcessor.class.getName(), true, false),
                new ClassFacts("io.quarkus.maven.structure.FooSubstitution", false, true),
                new ClassFacts(FooFeature.class.getName(), false, true),
                new ClassFacts(FooRecorder.class.getName(), false, false));
    }

    private static InputStream fixture(String path) {
        return ExtensionStructureScannerTest.class.getClassLoader().getResourceAsStream(path);
    }
}
