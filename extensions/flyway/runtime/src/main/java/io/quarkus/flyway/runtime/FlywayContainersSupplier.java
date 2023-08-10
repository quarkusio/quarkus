package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.flywaydb.core.Flyway;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public class FlywayContainersSupplier implements Supplier<Collection<FlywayContainer>> {

    @Override
    public Collection<FlywayContainer> get() {
        InjectableInstance<Flyway> instances = Arc.container().select(Flyway.class);
        for (Flyway instance : instances) { // force the creation of flyway

        }
        if (FlywayRecorder.FLYWAY_CONTAINERS.isEmpty()) {
            return Collections.emptySet();
        }

        Set<FlywayContainer> containers = new TreeSet<>(FlywayContainerComparator.INSTANCE);
        containers.addAll(FlywayRecorder.FLYWAY_CONTAINERS);
        return containers;
    }

    private static class FlywayContainerComparator implements Comparator<FlywayContainer> {

        private static final FlywayContainerComparator INSTANCE = new FlywayContainerComparator();

        @Override
        public int compare(FlywayContainer o1, FlywayContainer o2) {
            String dataSourceName1 = o1.getDataSourceName();
            String dataSourceName2 = o2.getDataSourceName();
            if (DataSourceUtil.isDefault(dataSourceName1)) {
                return -1;
            }
            if (DataSourceUtil.isDefault(dataSourceName2)) {
                return 1;
            }
            return dataSourceName1.compareTo(dataSourceName2);
        }

    }
}
