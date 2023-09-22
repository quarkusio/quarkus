package io.quarkus.it.main;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.BuildOutput;

@QuarkusIntegrationTest
public class ImageMetricsITCase {
    @Test
    public void verifyImageMetrics() {
        BuildOutput buildOutput = new BuildOutput();
        buildOutput.verifyImageMetrics();
    }
}
