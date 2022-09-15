package io.quarkus.liquibase.runtime.devconsole;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.liquibase.LiquibaseFactory;

public class LiquibaseFactoriesSupplier implements Supplier<Collection<LiquibaseFactory>> {

    @Override
    public Collection<LiquibaseFactory> get() {
        InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container().select(LiquibaseFactory.class,
                Any.Literal.INSTANCE);
        if (liquibaseFactoryInstance.isUnsatisfied()) {
            return Collections.emptySet();
        }

        Set<LiquibaseFactory> liquibaseFactories = new TreeSet<>(LiquibaseFactoryComparator.INSTANCE);
        for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
            liquibaseFactories.add(liquibaseFactoryHandle.get());
        }
        return liquibaseFactories;
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
