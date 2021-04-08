package io.quarkus.deployment;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CapabilityNameTest {

    @Test
    public void testName() {
        assertAll(
                () -> assertEquals("io.quarkus.agroal", Capability.AGROAL.getName()),
                () -> assertEquals("io.quarkus.security.jpa", Capability.SECURITY_JPA.getName()),
                () -> assertEquals("io.quarkus.container.image.docker", Capability.CONTAINER_IMAGE_DOCKER.getName()));
    }

}
