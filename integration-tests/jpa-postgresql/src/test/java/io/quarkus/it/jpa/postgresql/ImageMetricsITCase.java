package io.quarkus.it.jpa.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.NativeBuildOutputExtension;

@ExtendWith(NativeBuildOutputExtension.class)
@QuarkusIntegrationTest
public class ImageMetricsITCase {

    @Test
    public void verifyImageMetrics() {
        NativeBuildOutputExtension buildOutput = new NativeBuildOutputExtension();
        buildOutput.verifyImageMetrics();
    }
}
