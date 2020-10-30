package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.StackdriverMeterRegistryProvider;

/**
 * Add support for the Stackdriver Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class StackdriverRegistryProcessor {
    private static final Logger log = Logger.getLogger(StackdriverRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.stackdriver.StackdriverMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    static class StackdriverEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.stackdriver);
        }
    }

    @BuildStep(onlyIf = { NativeBuild.class, StackdriverEnabled.class })
    MicrometerRegistryProviderBuildItem nativeModeNotSupported() {
        log.info("The Stackdriver meter registry does not support running in native mode.");
        return null;
    }

    /** Stackdriver does not work with GraalVM */
    @BuildStep(onlyIf = StackdriverEnabled.class, onlyIfNot = NativeBuild.class)
    MicrometerRegistryProviderBuildItem createStackdriverRegistry(CombinedIndexBuildItem index,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the Stackdriver Registry Producer
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(StackdriverMeterRegistryProvider.class)
                .setUnremovable().build());

        // Include the StackdriverMeterRegistry in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }
}
