package io.quarkus.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

class MongoClientProcessorTest {
    private final MongoClientProcessor buildStep = new MongoClientProcessor();

    @SuppressWarnings("unchecked")
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

        BuildProducer<AdditionalIndexedClassesBuildItem> buildProducer = mock(BuildProducer.class);
        buildStep.includeMongoCommandMetricListener(buildProducer, config, capability);

        if (expectedResult) {
            var captor = ArgumentCaptor.forClass(AdditionalIndexedClassesBuildItem.class);
            verify(buildProducer, times(1)).produce(captor.capture());
            assertThat(captor.getAllValues().get(0).getClassesToIndex())
                    .containsExactly("io.quarkus.mongodb.metrics.MicrometerCommandListener");
        } else {
            verify(buildProducer, never()).produce(any(AdditionalIndexedClassesBuildItem.class));
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
