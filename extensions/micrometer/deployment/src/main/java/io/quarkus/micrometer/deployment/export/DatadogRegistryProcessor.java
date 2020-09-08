package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.DatadogMeterRegistryProvider;

/**
 * Add support for the Datadog Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class DatadogRegistryProcessor {
    static final String REGISTRY_CLASS_NAME = "io.micrometer.datadog.DatadogMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    static class DatadogEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.datadog);
        }
    }

    @BuildStep(onlyIf = DatadogEnabled.class)
    MicrometerRegistryProviderBuildItem createDatadogRegistry(CombinedIndexBuildItem index,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the Datadog Registry Producer
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(DatadogMeterRegistryProvider.class)
                .setUnremovable().build());

        // Include the DatadogMeterRegistry in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }
}
