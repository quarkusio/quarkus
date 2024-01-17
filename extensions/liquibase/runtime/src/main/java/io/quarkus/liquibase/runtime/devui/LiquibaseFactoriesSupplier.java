package io.quarkus.liquibase.runtime.devui;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseFactoryUtil;

public class LiquibaseFactoriesSupplier implements Supplier<Collection<LiquibaseFactory>> {

    @Override
    public Collection<LiquibaseFactory> get() {
        Set<LiquibaseFactory> containers = new TreeSet<>(LiquibaseFactoryComparator.INSTANCE);
        for (InstanceHandle<LiquibaseFactory> handle : LiquibaseFactoryUtil.getActiveLiquibaseFactories()) {
            containers.add(handle.get());
        }
        return containers;
    }

    private static class LiquibaseFactoryComparator implements Comparator<LiquibaseFactory> {

        private static final LiquibaseFactoryComparator INSTANCE = new LiquibaseFactoryComparator();

        @Override
        public int compare(LiquibaseFactory o1, LiquibaseFactory o2) {
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
