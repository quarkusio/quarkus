package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.SignalFxMeterRegistryProvider;

/**
 * Add support for the SignalFx Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class SignalFxRegistryProcessor {
    private static final Logger log = Logger.getLogger(SignalFxRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.signalfx.SignalFxMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class SignalFxRegistryEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        @Override
        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.signalfx);
        }
    }

    @BuildStep(onlyIf = SignalFxRegistryEnabled.class)
    MicrometerRegistryProviderBuildItem createSignalFxRegistry(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the SignalFx Registry Producer
        additionalBeans.produce(
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(SignalFxMeterRegistryProvider.class)
                        .setUnremovable().build());

        // Include the SignalFxMeterRegistryProvider in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }
}
