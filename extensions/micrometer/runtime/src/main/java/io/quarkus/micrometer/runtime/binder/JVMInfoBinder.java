package io.quarkus.micrometer.runtime.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

public class JVMInfoBinder implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("jvm_info", () -> 1L)
                .description("JVM version info")
                .tags("version", System.getProperty("java.runtime.version", "unknown"),
                        "vendor", System.getProperty("java.vm.vendor", "unknown"),
                        "runtime", System.getProperty("java.runtime.name", "unknown"))
                .strongReference(true)
                .register(registry);
    }
}
