package io.quarkus.micrometer.runtime.binder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

public class JVMInfoBinder implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        Counter.builder("jvm.info")
                .description("JVM version info")
                .tags("version", System.getProperty("java.runtime.version", "unknown"),
                        "vendor", System.getProperty("java.vm.vendor", "unknown"),
                        "runtime", System.getProperty("java.runtime.name", "unknown"))
                .register(registry)
                .increment();
    }
}
