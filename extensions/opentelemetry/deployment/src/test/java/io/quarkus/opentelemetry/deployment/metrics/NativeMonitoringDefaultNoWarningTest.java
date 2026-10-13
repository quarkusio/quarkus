package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

class NativeMonitoringDefaultNoWarningTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setApplicationName("otel-native-monitoring-default")
            .setApplicationVersion("1.0.0")
            .setBuildNative(true)
            .overrideConfigKey("quarkus.native.sources-only", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .setRun(false)
            .setLogRecordPredicate(r -> r.getLoggerName().contains("MetricProcessor")
                    && r.getLevel().intValue() >= Level.WARNING.intValue());

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void doesNotWarn() {
        assertThat(prodModeTestResults.getRetainedBuildLogRecords()).isEmpty();
    }
}
