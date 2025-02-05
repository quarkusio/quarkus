package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.NativeMonitoringBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * Add support for virtual thread metric collections.
 */
public class VirtualThreadBinderProcessor {
    static final String VIRTUAL_THREAD_COLLECTOR_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.virtualthreads.VirtualThreadCollector";

    static final String VIRTUAL_THREAD_BINDER_CLASS_NAME = "io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics";
    static final Class<?> VIRTUAL_THREAD_BINDER_CLASS = MicrometerRecorder.getClassForName(VIRTUAL_THREAD_BINDER_CLASS_NAME);

    static class VirtualThreadSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return VIRTUAL_THREAD_BINDER_CLASS != null // The binder is in another Micrometer artifact
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder().virtualThreads());
        }
    }

    @BuildStep(onlyIf = VirtualThreadSupportEnabled.class)
    AdditionalBeanBuildItem createCDIEventConsumer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(VIRTUAL_THREAD_COLLECTOR_CLASS_NAME)
                .setUnremovable().build();
    }

    @BuildStep(onlyIf = VirtualThreadSupportEnabled.class)
    void addNativeMonitoring(BuildProducer<NativeMonitoringBuildItem> nativeMonitoring) {
        nativeMonitoring.produce(new NativeMonitoringBuildItem(NativeConfig.MonitoringOption.JFR));
    }
}
