package org.acme.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ApplicationModelTest {

    @Test
    void deploymentTestReceivesApplicationModel() throws Exception {
        String model = System.getProperty("quarkus-internal-test.serialized-app-model.path");
        assertTrue(model != null && Files.isRegularFile(Path.of(model)));
        Files.writeString(Path.of("build/migration-test-model.txt"), model);
    }
}
