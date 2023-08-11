package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public class FlywayContainersSupplier implements Supplier<Collection<FlywayContainer>> {

    @Override
    public Collection<FlywayContainer> get() {
        List<InstanceHandle<FlywayContainer>> flywayContainerHandles = Arc.container().listAll(FlywayContainer.class);

        if (flywayContainerHandles.isEmpty()) {
            return Set.of();
        }

        Set<FlywayContainer> containers = new TreeSet<>(FlywayContainerComparator.INSTANCE);
        for (InstanceHandle<FlywayContainer> flywayContainerHandle : flywayContainerHandles) {
            containers.add(flywayContainerHandle.get());
        }
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
