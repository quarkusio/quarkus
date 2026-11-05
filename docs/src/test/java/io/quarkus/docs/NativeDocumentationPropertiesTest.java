package io.quarkus.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fabric8.maven.Maven;
import io.quarkus.deployment.images.ContainerImages;
import io.quarkus.runtime.graal.GraalVM;

/**
 * Guards the {@code *-for-documentation} properties of {@code docs/pom.xml} against drift from the code.
 * <p>
 * The guides read these properties through {@code _attributes.adoc} ({@code graalvm-version},
 * {@code graalvm-docs-version}, {@code graalvm-flavor} and {@code mandrel-flavor}). The image tags must match
 * the builder images the code uses by default, and the GraalVM version must name the Java version of the
 * GraalVM release the code is tested with.
 */
public class NativeDocumentationPropertiesTest {

    private static final Pattern JDK_VERSION = Pattern.compile("(?i)jdk[ -]?(\\d+)");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private static Properties properties;

    @BeforeAll
    static void readDocsPom() throws URISyntaxException {
        // target/test-classes -> docs
        Path testClasses = Path.of(NativeDocumentationPropertiesTest.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI());
        Model model = Maven.readModel(testClasses.resolve("../../pom.xml").normalize());
        assertEquals("quarkus-documentation", model.getArtifactId());
        properties = model.getProperties();
    }

    @Test
    public void mandrelImageTagMatchesTheDefaultBuilderImage() {
        assertEquals(ContainerImages.UBI10_MANDREL_BUILDER_VERSION, property("mandrel.image-tag-for-documentation"),
                "docs/pom.xml property mandrel.image-tag-for-documentation must match "
                        + "ContainerImages.UBI10_MANDREL_BUILDER_VERSION");
        assertEquals(ContainerImages.UBI10_MANDREL_BUILDER_VERSION, ContainerImages.UBI9_MANDREL_BUILDER_VERSION,
                "The guides use {mandrel-flavor} for the UBI 9 and the UBI 10 Mandrel builder images; "
                        + "now that their tags differ, the UBI 9 references need an attribute of their own");
    }

    @Test
    public void graalvmImageTagMatchesTheGraalvmBuilderImage() {
        assertEquals(ContainerImages.UBI10_GRAALVM_BUILDER_VERSION, property("graal-community.image-tag-for-documentation"),
                "docs/pom.xml property graal-community.image-tag-for-documentation must match "
                        + "ContainerImages.UBI10_GRAALVM_BUILDER_VERSION");
    }

    @ParameterizedTest
    @ValueSource(strings = { "graal-community.tag-for-documentation", "graal-community.version-for-documentation" })
    public void graalvmVersionNamesTheCurrentJavaVersion(String name) {
        String value = property(name);
        Matcher matcher = JDK_VERSION.matcher(value);
        assertTrue(matcher.find(), "docs/pom.xml property " + name + " must name a JDK version, but is '" + value + "'");
        assertEquals(GraalVM.Version.CURRENT.javaVersion.feature(), Integer.parseInt(matcher.group(1)),
                "docs/pom.xml property " + name + " must name the Java version of GraalVM.Version.CURRENT");
    }

    private static String property(String name) {
        return property(name, 0);
    }

    private static String property(String name, int depth) {
        assertTrue(depth < 10, "docs/pom.xml property " + name + " references itself");
        String value = properties.getProperty(name);
        assertNotNull(value, "docs/pom.xml has no property " + name + " (only ${...} references to properties of "
                + "docs/pom.xml itself are resolved)");
        Matcher placeholder = PLACEHOLDER.matcher(value);
        while (placeholder.find()) {
            value = value.replace(placeholder.group(), property(placeholder.group(1), depth + 1));
            placeholder = PLACEHOLDER.matcher(value);
        }
        return value;
    }
}
