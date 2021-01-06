package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.function.Supplier;

public class FlywayContainerSupplier implements Supplier<List<FlywayContainer>> {

    @Override
    public List<FlywayContainer> get() {
        return FlywayRecorder.flywayContainers;
    }
}
