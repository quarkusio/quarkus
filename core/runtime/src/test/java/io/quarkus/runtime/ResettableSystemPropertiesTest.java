package io.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ResettableSystemPropertiesTest {

    @Test
    public void happyPath() {
        System.setProperty("prop1", "val1");
        assertThat(System.getProperty("prop1")).isEqualTo("val1");
        try (var ignored = new ResettableSystemProperties(
                Map.of("prop1", "val11", "prop2", "val2"))) {
            assertThat(System.getProperty("prop1")).isEqualTo("val11");
            assertThat(System.getProperty("prop2")).isEqualTo("val2");
        }
        assertThat(System.getProperty("prop1")).isEqualTo("val1");
        assertThat(System.getProperties()).doesNotContainKey("prop2");
    }

    @Test
    public void exceptionThrown() {
        System.setProperty("prop1", "val1");
        int initCount = System.getProperties().size();
        assertThat(System.getProperty("prop1")).isEqualTo("val1");
        try (var ignored = new ResettableSystemProperties(
                Map.of("prop1", "val11", "prop2", "val2"))) {
            assertThat(System.getProperty("prop1")).isEqualTo("val11");
            assertThat(System.getProperty("prop2")).isEqualTo("val2");

            throw new RuntimeException("dummy");
        } catch (Exception ignored) {

        }
        assertThat(System.getProperty("prop1")).isEqualTo("val1");
        assertThat(System.getProperties()).doesNotContainKey("prop2");
        assertThat(System.getProperties().size()).isEqualTo(initCount);
    }
}
