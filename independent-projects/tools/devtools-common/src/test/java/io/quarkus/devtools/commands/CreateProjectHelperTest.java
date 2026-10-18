package io.quarkus.devtools.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CreateProjectHelperTest {

    @Test
    void testSanitizeExtensionsWithNull() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(null);
        assertThat(result).isEmpty();
    }

    @Test
    void testSanitizeExtensionsWithEmptySet() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of());
        assertThat(result).isEmpty();
    }

    @Test
    void testSanitizeExtensionsWithValidExtensions() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("rest", "hibernate-orm"));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm");
    }

    @Test
    void testSanitizeExtensionsWithWhitespace() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("  rest  ", " hibernate-orm "));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm");
    }

    @Test
    void testSanitizeExtensionsWithSingleQuotes() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("'rest'", "'hibernate-orm'"));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm");
    }

    @Test
    void testSanitizeExtensionsWithDoubleQuotes() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("\"rest\"", "\"hibernate-orm\""));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm");
    }

    @Test
    void testSanitizeExtensionsWithQuotesAndWhitespace() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("  'rest'  ", " \"hibernate-orm\" "));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm");
    }

    @Test
    void testSanitizeExtensionsWithMixedQuotesAndNoQuotes() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("rest", "'hibernate-orm'", "\"reactive-routes\""));
        assertThat(result).containsExactlyInAnyOrder("rest", "hibernate-orm", "reactive-routes");
    }

    @Test
    void testSanitizeExtensionsWithOnlyOpeningQuote() {
        // Should only strip matching quotes, not unmatched ones
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("'rest", "hibernate-orm\""));
        assertThat(result).containsExactlyInAnyOrder("'rest", "hibernate-orm\"");
    }

    @Test
    void testSanitizeExtensionsWithNestedQuotes() {
        // Should only strip outer quotes
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("'my\"extension\"'"));
        assertThat(result).containsExactlyInAnyOrder("my\"extension\"");
    }

    @Test
    void testSanitizeExtensionsWithEmptyString() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("", "rest", " "));
        assertThat(result).containsExactlyInAnyOrder("rest");
    }

    @Test
    void testSanitizeExtensionsWithEmptyQuotedString() {
        Set<String> result = CreateProjectHelper.sanitizeExtensions(Set.of("''", "\"\"", "rest"));
        assertThat(result).containsExactlyInAnyOrder("rest");
    }

    @Test
    void testSanitizeExtensionsWithFullArtifactCoords() {
        // Full artifact coordinates should work with quotes too
        Set<String> result = CreateProjectHelper.sanitizeExtensions(
                Set.of("'io.quarkus:quarkus-rest:3.0.0'", "org.acme:my-extension:1.0"));
        assertThat(result).containsExactlyInAnyOrder("io.quarkus:quarkus-rest:3.0.0", "org.acme:my-extension:1.0");
    }
}
