package io.quarkus.panache.rest.common.deployment.utils;

import static io.quarkus.panache.rest.common.deployment.utils.ResourceName.fromClass;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceNameTest {

    @Test
    void testNameWithoutSuffix() {
        assertThat(fromClass("SomeEntity")).isEqualTo("some-entity");
    }

    @Test
    void testResourceName() {
        assertThat(fromClass("SomeEntityResource")).isEqualTo("some-entity");
    }

    @Test
    void testControllerName() {
        assertThat(fromClass("SomeEntityController")).isEqualTo("some-entity");
    }
}