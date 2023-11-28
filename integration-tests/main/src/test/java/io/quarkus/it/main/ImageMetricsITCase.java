package io.quarkus.it.main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.BuildOutputExtension;

@ExtendWith(BuildOutputExtension.class)
@QuarkusIntegrationTest
public class ImageMetricsITCase {
    @Test
    public void verifyImageMetrics() {
        BuildOutputExtension buildOutput = new BuildOutputExtension();
        buildOutput.verifyImageMetrics();
    }
}
