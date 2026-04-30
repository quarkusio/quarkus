package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.cyclonedx.parsers.JsonParser;

final class CycloneDxTestUtils {

    private CycloneDxTestUtils() {
    }

    static Bom parseSbom(File testDir, String sbomFileName) throws Exception {
        final File sbomFile = new File(testDir, "target/" + sbomFileName);
        assertThat(sbomFile).exists();
        return new JsonParser().parse(sbomFile);
    }

    /**
     * Asserts that the main component is the quarkus-run.jar runner
     * (used by fast-jar and mutable-jar packaging).
     */
    static void assertRunnerMainComponent(Bom bom) {
        final Component mainComponent = bom.getMetadata().getComponent();
        assertThat(mainComponent).isNotNull();
        assertThat(mainComponent.getName()).isEqualTo("quarkus-run.jar");
        assertThat(mainComponent.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(mainComponent.getType()).isEqualTo(Component.Type.APPLICATION);
        assertThat(mainComponent.getPurl()).isEqualTo("pkg:generic/quarkus-run.jar@1.0-SNAPSHOT");
        assertComponentScope(mainComponent, "runtime");
    }

    /**
     * Asserts that a component with the given group and name exists in the SBOM,
     * has the expected scope property, and optionally has an evidence location
     * starting with the given prefix.
     *
     * @param components the component list
     * @param group expected group
     * @param name expected artifact name
     * @param expectedScope expected value of the quarkus:component:scope property
     * @param expectedLocationPrefix if non-null, the component's evidence location must start with this prefix;
     *        if null, no evidence location is expected
     */
    static void assertComponent(List<Component> components, String group, String name,
            String expectedScope, String expectedLocationPrefix) {
        final Component component = components.stream()
                .filter(c -> group.equals(c.getGroup()) && name.equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertThat(component)
                .as("Expected component %s:%s in SBOM", group, name)
                .isNotNull();
        assertComponentScope(component, expectedScope);
        assertEvidenceLocation(component, expectedLocationPrefix);
    }

    static void assertComponentScope(Component component, String expectedScope) {
        final List<Property> properties = component.getProperties();
        assertThat(properties).isNotNull();
        final String scope = properties.stream()
                .filter(p -> "quarkus:component:scope".equals(p.getName()))
                .map(Property::getValue)
                .findFirst()
                .orElse(null);
        assertThat(scope)
                .as("quarkus:component:scope of %s:%s", component.getGroup(), component.getName())
                .isEqualTo(expectedScope);
    }

    /**
     * Parses a CycloneDX JSON SBOM embedded as a resource inside a JAR file.
     */
    static Bom parseEmbeddedSbom(Path jarFile, String resourceName) throws Exception {
        assertThat(jarFile.toFile()).exists();
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            JarEntry entry = jar.getJarEntry(resourceName);
            assertThat(entry)
                    .as("Expected resource %s in %s", resourceName, jarFile.getFileName())
                    .isNotNull();
            try (InputStream is = jar.getInputStream(entry)) {
                return new JsonParser().parse(is);
            }
        }
    }

    /**
     * Parses a GZIP-compressed CycloneDX JSON SBOM embedded as a resource inside a JAR file.
     */
    static Bom parseCompressedEmbeddedSbom(Path jarFile, String resourceName) throws Exception {
        assertThat(jarFile.toFile()).exists();
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            JarEntry entry = jar.getJarEntry(resourceName);
            assertThat(entry)
                    .as("Expected resource %s in %s", resourceName, jarFile.getFileName())
                    .isNotNull();
            try (InputStream is = new GZIPInputStream(jar.getInputStream(entry))) {
                return new JsonParser().parse(is);
            }
        }
    }

    /**
     * Asserts that a JAR file does not contain a resource with the given name.
     */
    static void assertNoEmbeddedResource(Path jarFile, String resourceName) throws Exception {
        assertThat(jarFile.toFile()).exists();
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            assertThat(jar.getJarEntry(resourceName))
                    .as("Resource %s should not exist in %s", resourceName, jarFile.getFileName())
                    .isNull();
        }
    }

    private static void assertEvidenceLocation(Component component, String expectedLocationPrefix) {
        if (expectedLocationPrefix == null) {
            if (component.getEvidence() == null || component.getEvidence().getOccurrences() == null) {
                return;
            }
            assertThat(component.getEvidence().getOccurrences())
                    .as("Evidence locations of %s:%s", component.getGroup(), component.getName())
                    .isEmpty();
            return;
        }
        assertThat(component.getEvidence()).isNotNull();
        final List<Occurrence> occurrences = component.getEvidence().getOccurrences();
        assertThat(occurrences)
                .as("Evidence locations of %s:%s", component.getGroup(), component.getName())
                .isNotEmpty();
        assertThat(occurrences.get(0).getLocation()).startsWith(expectedLocationPrefix);
    }
}
