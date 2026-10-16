package org.acme;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Fourth test in the sequence: uses a different profile with a different
 * devservices port, making the container configuration genuinely
 * incompatible with the first test's container.
 */
@QuarkusTest
@TestProfile(ContainerIdRecorderTest.Profile3.class)
@Order(4)
public class ContainerIdIncompatibleProfileTest {

    static final String CONTAINER_ID_FILE = "target/container-id-incompatible-profile.txt";

    @ConfigProperty(name = "acme.simpleextension.container-id")
    String containerId;

    @Test
    void recordContainerId() throws IOException {
        assertNotNull(containerId);
        Files.writeString(Path.of(CONTAINER_ID_FILE), containerId);
    }
}
