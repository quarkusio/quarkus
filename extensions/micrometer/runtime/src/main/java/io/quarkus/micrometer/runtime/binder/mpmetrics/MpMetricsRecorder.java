package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.MetricRegistry;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MpMetricsRecorder {

    static Map<MetricRegistry.Type, MetricRegistryAdapter> registries = new HashMap<>(3);

    /* STATIC INIT */
    public void configureRegistryAdapter(RuntimeValue<MeterRegistry> registryRuntimeValue) {
        MeterRegistry registry = registryRuntimeValue.getValue();

        for (MetricRegistry.Type type : MetricRegistry.Type.values()) {
            registries.put(type, new MetricRegistryAdapter(type, registry));
        }
    }

    static MetricRegistryAdapter getRegistry(MetricRegistry.Type type) {
        return registries.get(type);
    }
}
