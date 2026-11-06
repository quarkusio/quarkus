package io.quarkus.it.netty;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class NettySizingIT extends NettySizingTest {

    @Test
    @Launch
    public void artifactRunsWithTheConfiguredArgLine(LaunchResult result) {
        // Otherwise the sizing assertions could not tell build-time values from run-time ones
        assertThat(values(result))
                .containsEntry("runtime.availableProcessors", "1")
                .containsEntry("runtime.maxMemory", String.valueOf(256 * 1024 * 1024))
                .containsEntry("runtime.magazineBufferQueueCapacity", "77");
    }
}
