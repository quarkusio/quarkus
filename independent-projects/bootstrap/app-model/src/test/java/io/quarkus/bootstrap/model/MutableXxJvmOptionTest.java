package io.quarkus.bootstrap.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class MutableXxJvmOptionTest {

    @Test
    public void testHasNoValue() {
        assertThat(MutableXxJvmOption.newInstance("AllowUserSignalHandlers").hasValue()).isFalse();
    }

    @Test
    public void testHasValue() {
        assertThat(MutableXxJvmOption.newInstance("AllowUserSignalHandlers", "true").hasValue()).isTrue();
    }

    @Test
    public void testCliBooleanOptionWithNoValue() {
        assertThat(MutableXxJvmOption.newInstance("AllowUserSignalHandlers").toCliOptions())
                .containsExactly("-XX:+AllowUserSignalHandlers");
    }

    @Test
    public void testCliBooleanOptionWithTrueValue() {
        assertThat(MutableXxJvmOption.newInstance("AllowUserSignalHandlers", "true").toCliOptions())
                .containsExactly("-XX:+AllowUserSignalHandlers");
    }

    @Test
    public void testCliBooleanOptionWithFalseValue() {
        assertThat(MutableXxJvmOption.newInstance("AllowUserSignalHandlers", "false").toCliOptions())
                .containsExactly("-XX:-AllowUserSignalHandlers");
    }

    @Test
    public void testCliOptionWithSingleStringValue() {
        assertThat(MutableXxJvmOption.newInstance("ErrorFile", "./hs_err_pid<pid>.log").toCliOptions())
                .containsExactly("-XX:ErrorFile=./hs_err_pid<pid>.log");
    }
}
