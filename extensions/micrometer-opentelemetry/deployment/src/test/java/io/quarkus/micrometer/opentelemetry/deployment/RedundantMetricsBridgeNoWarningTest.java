package io.quarkus.micrometer.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class RedundantMetricsBridgeNoWarningTest {

    @SuppressWarnings("Convert2Lambda")
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // Conditions met for a warning: Registry provider present AND OTel metrics enabled.
            // The bridge unifies these under MICROMETER_OPENTELEMETRY, so we must explicitly
            // suppress the warning via exemption (instead of relying on a missing registry).
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder builder) {
                    builder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new MicrometerRegistryProviderBuildItem(SimpleMeterRegistry.class));
                        }
                    }).produces(MicrometerRegistryProviderBuildItem.class).build();
                }
            })
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && MicrometerProcessor.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .isEmpty());

    @Test
    void bridgeIsExemptFromRedundancyWarning() {
    }
}
