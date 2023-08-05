package io.quarkus.it.init;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class InitTaskIT {

    @Test
    public void logShouldIncludeInitMessages() {
        final Path path = Paths.get(".", "target", "quarkus.log").toAbsolutePath();
        org.awaitility.Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTrue(Files.exists(path), "Quarkus log file " + path + " is missing");
                    boolean found = false;
                    List<String> lines = Files.readAllLines(path);
                    // Check that PreStart on Runnable does work
                    assertTrue(lines.stream().anyMatch(l -> l.contains(LoggingInitRunnable.class.getSimpleName())));
                    // Check that unamed PreStart on method works too
                    assertTrue(lines.stream().anyMatch(l -> l.contains(LoggingInitMethod.class.getSimpleName())));
                });
    }
}
