package io.quarkus.smallrye.metrics.test;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verify that when Micrometer compatibility mode for metrics is enabled,
 * then appropriate metrics (equivalents of "jvm" Micrometer metrics) are exposed.
 */
public class MicrometerMetricsCompatibilityModeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-metrics.micrometer.compatibility=true"),
                            "application.properties"));

    @Test
    public void verifyOpenMetricsExport() {
        RestAssured.when().get("/q/metrics").then()
                .body(containsString("jvm_memory_max_bytes{"),
                        containsString("jvm_memory_used_bytes{"),
                        containsString("jvm_memory_committed_bytes{"),
                        containsString("jvm_gc_memory_promoted_bytes_total "),
                        containsString("jvm_gc_live_data_size_bytes "),
                        containsString("jvm_buffer_count_buffers{"),
                        containsString("jvm_buffer_total_capacity_bytes{"),
                        containsString("jvm_threads_states_threads{"),
                        containsString("jvm_threads_daemon_threads "),
                        containsString("jvm_threads_live_threads "),
                        containsString("jvm_threads_peak_threads "),
                        containsString("jvm_classes_loaded_classes "));
    }

}
