package io.quarkus.devtools.codestarts.core.reader;

import static io.quarkus.devtools.codestarts.core.reader.QuteCodestartFileReader.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QuteCodestartFileReaderTest {

    @Test
    void testCompareVersion() {
        assertThat(compareVersionTo("3.12.0.Final", "1.0")).isGreaterThan(0);
        assertThat(compareVersionTo("3.12.0.Final", "3.12")).isEqualTo(0);
        assertThat(compareVersionTo("3.13.0", "3.12")).isGreaterThan(0);
        assertThat(compareVersionTo("3.2.1", "3.12")).isLessThan(0);
        assertThat(compareVersionTo("999-SNAPSHOT", "3.12")).isGreaterThan(0);
        assertThat(compareVersionTo("1.0.0.Final-redhat-00001", "1.0")).isGreaterThan(0);
        assertThat(compareVersionTo("1.0.1.Final-redhat-00001", "1.0")).isGreaterThan(0);
        assertThatThrownBy(() -> compareVersionTo("999-SNAP", "1.0.1.Final-redhat-00001"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
