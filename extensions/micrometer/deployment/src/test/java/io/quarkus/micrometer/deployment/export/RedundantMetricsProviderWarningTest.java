package io.quarkus.micrometer.deployment.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class RedundantMetricsProviderWarningTest {

    @SuppressWarnings("Convert2Lambda")
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            // No built-in registry: only the synthetic provider below should trip the warning.
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // Stand in for a third-party registry that only produces the SPI build item.
            .addBuildChainCustomizer((new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder builder) {
                    builder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new MicrometerRegistryProviderBuildItem(SimpleMeterRegistry.class));
                        }
                    }).produces(MicrometerRegistryProviderBuildItem.class).build();
                }
            }))
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && MicrometerProcessor.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .singleElement()
                    .satisfies(
                            record -> {
                                assertThat(record.getMessage()).startsWith("Redundant")
                                        .contains("Micrometer (registries:", "and OpenTelemetry");
                                assertThat(record.getParameters()).singleElement().asString().contains("SimpleMeterRegistry");
                            }));

    @Test
    void warnsForAnyRegistryProvider() {
    }
}
