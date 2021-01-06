package io.quarkus.liquibase.runtime;

import java.util.List;
import java.util.function.Supplier;

public class LiquibaseContainerSupplier implements Supplier<List<LiquibaseContainer>> {

    @Override
    public List<LiquibaseContainer> get() {
        return LiquibaseRecorder.liquibaseContainers;
    }
}
