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
 * Second test in the sequence: uses the same profile as
 * {@link ContainerIdRecorderTest} and records the container ID
 * for later comparison.
 */
@QuarkusTest
@TestProfile(ContainerIdRecorderTest.Profile1.class)
@Order(2)
public class ContainerIdSameProfileTest {

    static final String CONTAINER_ID_FILE = "target/container-id-same-profile.txt";

    @ConfigProperty(name = "acme.simpleextension.container-id")
    String containerId;

    @Test
    void recordContainerId() throws IOException {
        assertNotNull(containerId);
        Files.writeString(Path.of(CONTAINER_ID_FILE), containerId);
    }
}
