package io.quarkus.gradle.application.internal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class ManifestConfigPropertiesTest {

    private static final String CONTEXT = "named build 'app' task ':quarkusAppBuild'";

    @Test
    void convertsAttributesAndSectionsInStableOrder() {
        assertThat(ManifestConfigProperties.attributes(CONTEXT, Map.of(
                "Z-Attribute", "last",
                "A-Attribute", "first")))
                .containsExactly(
                        entry("quarkus.package.jar.manifest.attributes.\"A-Attribute\"", "first"),
                        entry("quarkus.package.jar.manifest.attributes.\"Z-Attribute\"", "last"));

        assertThat(ManifestConfigProperties.section(CONTEXT, "META-INF/services", Map.of(
                "Z-Attribute", "last",
                "A-Attribute", "first")))
                .containsExactly(
                        entry("quarkus.package.jar.manifest.sections.\"META-INF/services\".\"A-Attribute\"", "first"),
                        entry("quarkus.package.jar.manifest.sections.\"META-INF/services\".\"Z-Attribute\"", "last"));
    }

    @Test
    void rejectsInvalidAttributeNamesWithContext() {
        assertThatThrownBy(() -> ManifestConfigProperties.attributes(CONTEXT, Map.of("invalid name", "value")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("invalid name")
                .hasMessageContaining(CONTEXT);

        assertThatThrownBy(() -> ManifestConfigProperties.attributes(CONTEXT, Map.of("invalid\"name", "value")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("double quotes")
                .hasMessageContaining(CONTEXT);
    }

    @Test
    void rejectsCaseInsensitiveDuplicateAttributeNames() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("Built-By", "first");
        attributes.put("built-by", "second");

        assertThatThrownBy(() -> ManifestConfigProperties.attributes(CONTEXT, attributes))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Built-By")
                .hasMessageContaining("built-by")
                .hasMessageContaining("differ only by case")
                .hasMessageContaining(CONTEXT);
    }

    @Test
    void rejectsInvalidSectionNamesWithContext() {
        assertInvalidSection("");
        assertInvalidSection(" ");
        assertInvalidSection("invalid\"section");
        assertInvalidSection("invalid\nsection");
        assertInvalidSection("invalid\u0000section");
    }

    private static void assertInvalidSection(String section) {
        assertThatThrownBy(() -> ManifestConfigProperties.section(CONTEXT, section, Map.of("Built-By", "value")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Manifest section name")
                .hasMessageContaining(CONTEXT);
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
    }
}
