package io.quarkus.rest.data.panache.deployment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceNameTest {

    @Test
    void testNameWithoutSuffix() {
        assertThat(ResourceName.fromClass("SomeEntity")).isEqualTo("some-entity");
    }

    @Test
    void testResourceName() {
        assertThat(ResourceName.fromClass("SomeEntityResource")).isEqualTo("some-entity");
    }

    @Test
    void testControllerName() {
        assertThat(ResourceName.fromClass("SomeEntityController")).isEqualTo("some-entity");
    }

    @Test
    void testComplexName() {
        assertThat(ResourceName.fromClass("com.example.Entity")).isEqualTo("entity");
    }
}