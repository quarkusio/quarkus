package io.quarkus.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

class MongoClientProcessorTest {
    private final MongoClientProcessor buildStep = new MongoClientProcessor();

    @ParameterizedTest
    @CsvSource({
            "true, true, true", // Metrics enabled and Micrometer supported
            "true, false, false", // Metrics enabled but Micrometer not supported
            "false, true, false", // Metrics disabled and Micrometer supported
            "false, false, false" // Metrics disabled and Micrometer not supported
    })
    void testIncludeMongoCommandMetricListener(boolean metricsEnabled, boolean micrometerSupported, boolean expectedResult) {
        MongoClientBuildTimeConfig config = config(metricsEnabled);
        Optional<MetricsCapabilityBuildItem> capability = capability(metricsEnabled, micrometerSupported);

        AdditionalIndexedClassesBuildItem result = buildStep.includeMongoCommandMetricListener(config, capability);

        if (expectedResult) {
            assertThat(result.getClassesToIndex())
                    .containsExactly("io.quarkus.mongodb.metrics.MicrometerCommandListener");
        } else {
            assertThat(result.getClassesToIndex()).isEmpty();
        }
    }

    private static Optional<MetricsCapabilityBuildItem> capability(boolean metricsEnabled, boolean micrometerSupported) {
        MetricsCapabilityBuildItem capability = metricsEnabled
                ? new MetricsCapabilityBuildItem(factory -> MetricsFactory.MICROMETER.equals(factory) && micrometerSupported)
                : null;
        return Optional.ofNullable(capability);
    }

    private static MongoClientBuildTimeConfig config(boolean metricsEnabled) {
        MongoClientBuildTimeConfig buildTimeConfig = new MongoClientBuildTimeConfig();
        buildTimeConfig.metricsEnabled = metricsEnabled;
        return buildTimeConfig;
    }

}
