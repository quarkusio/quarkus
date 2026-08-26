package org.acme;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * First test in the profile-change sequence: records the container ID
 * so subsequent tests can verify reuse or replacement.
 */
@QuarkusTest
@TestProfile(ContainerIdRecorderTest.Profile1.class)
@Order(1)
public class ContainerIdRecorderTest {

    static final String CONTAINER_ID_FILE = "target/container-id-first.txt";

    @ConfigProperty(name = "acme.simpleextension.container-id")
    String containerId;

    @Test
    void recordContainerId() throws IOException {
        assertNotNull(containerId);
        Files.writeString(Path.of(CONTAINER_ID_FILE), containerId);
    }

    public static class Profile1 implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile1";
        }
    }

    public static class Profile2 implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile2";
        }
    }

    public static class Profile3 implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile3";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.simple-extension.devservices.port", "57433");
        }
    }
}
