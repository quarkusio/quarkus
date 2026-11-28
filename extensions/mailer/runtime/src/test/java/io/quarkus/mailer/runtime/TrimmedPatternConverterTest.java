package io.quarkus.mailer.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class TrimmedPatternConverterTest {

    private final TrimmedPatternConverter converter = new TrimmedPatternConverter();

    @Test
    void keepsRegexCharacterClasses() {
        var pattern = converter.convert(" \\S+@example\\.com ");

        assertThat(pattern.pattern()).isEqualTo("\\S+@example\\.com");
        assertThat("user@example.com").matches(pattern);
        assertThat("User@Example.com").matches(pattern);
        assertThat(" @example.com").doesNotMatch(pattern);
    }

    @Test
    void doesNotLowercaseThePatternSource() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            var pattern = converter.convert(".*@QUARKUS\\.IO");

            assertThat(pattern.pattern()).isEqualTo(".*@QUARKUS\\.IO");
            assertThat("user@quarkus.io").matches(pattern);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void blankValueIsEmpty() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert("   ")).isNull();
    }
}
