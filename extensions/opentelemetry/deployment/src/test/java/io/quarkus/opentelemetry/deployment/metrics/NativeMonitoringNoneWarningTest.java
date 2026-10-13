package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

class NativeMonitoringNoneWarningTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setApplicationName("otel-native-monitoring-none")
            .setApplicationVersion("1.0.0")
            .setBuildNative(true)
            .overrideConfigKey("quarkus.native.sources-only", "true")
            .overrideConfigKey("quarkus.native.monitoring", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .setRun(false)
            .setLogRecordPredicate(r -> r.getLoggerName().contains("MetricProcessor")
                    && r.getLevel().intValue() >= Level.WARNING.intValue());

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void warnsThatJfrMetricsAreLost() {
        assertThat(prodModeTestResults.getRetainedBuildLogRecords())
                .anyMatch(r -> r.getMessage().contains("'quarkus.native.monitoring' is set to 'none'")
                        && r.getMessage().contains("JFR-based JVM metrics will not be produced"));
    }
}
