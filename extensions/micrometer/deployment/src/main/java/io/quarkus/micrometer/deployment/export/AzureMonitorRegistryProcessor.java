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
import io.quarkus.micrometer.runtime.export.AzureMonitorMeterRegistryProvider;

/**
 * Add support for the Azure Monitor Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class AzureMonitorRegistryProcessor {
    private static final Logger log = Logger.getLogger(AzureMonitorRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.azuremonitor.AzureMonitorMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class AzureMonitorEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        @Override
        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.azuremonitor);
        }
    }

    @BuildStep(onlyIf = { NativeBuild.class, AzureMonitorEnabled.class })
    MicrometerRegistryProviderBuildItem nativeModeNotSupported() {
        log.info("The Azure Monitor meter registry does not support running in native mode.");
        return null;
    }

    @BuildStep(onlyIf = AzureMonitorEnabled.class, onlyIfNot = NativeBuild.class)
    MicrometerRegistryProviderBuildItem createAzureMonitorRegistry(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the AzureMonitor Registry Producer
        additionalBeans.produce(
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(AzureMonitorMeterRegistryProvider.class)
                        .setUnremovable().build());

        // Include the AzureMonitorMeterRegistry in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }
}
