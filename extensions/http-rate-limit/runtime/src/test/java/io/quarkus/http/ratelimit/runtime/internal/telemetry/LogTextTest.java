package io.quarkus.http.ratelimit.runtime.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogTextTest {

    @Test
    void plainTextIsUnchanged() {
        String text = "path:/api/users/é";
        assertThat(LogText.escape(text)).isSameAs(text);
    }

    @Test
    void controlCharactersAndSeparatorsAreEscaped() {
        assertThat(LogText.escape("a\r\nINFO forged\t\u001b[31m\u0000\u007f \\"))
                .isEqualTo("a\\r\\nINFO forged\\t\\u001b[31m\\u0000\\u007f\\u2028\\\\");
    }
}
