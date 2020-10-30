package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.StatsdMeterRegistryProvider;

/**
 * Add support for the StatsD Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class StatsdRegistryProcessor {
    private static final Logger log = Logger.getLogger(StatsdRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.statsd.StatsdMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class StatsdRegistryEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        @Override
        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.statsd);
        }
    }

    @BuildStep(onlyIf = { NativeBuild.class, StatsdRegistryEnabled.class })
    MicrometerRegistryProviderBuildItem nativeModeNotSupported() {
        log.info("The StatsD meter registry does not support running in native mode.");
        return null;
    }

    @BuildStep(onlyIf = StatsdRegistryEnabled.class, onlyIfNot = NativeBuild.class)
    MicrometerRegistryProviderBuildItem createStatsdRegistry(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the Statsd Registry Producer
        additionalBeans.produce(
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(StatsdMeterRegistryProvider.class)
                        .setUnremovable().build());

        // Include the StatsdMeterRegistryProvider in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }

}
