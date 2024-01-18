package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Default;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.flyway.FlywayDataSource;

public final class FlywayContainerUtil {
    private FlywayContainerUtil() {
    }

    public static FlywayContainer getFlywayContainer(String dataSourceName) {
        return Arc.container().instance(FlywayContainer.class,
                getFlywayContainerQualifier(dataSourceName)).get();
    }

    public static List<FlywayContainer> getActiveFlywayContainers() {
        List<FlywayContainer> result = new ArrayList<>();
        for (String datasourceName : Arc.container().instance(DataSources.class).get().getActiveDataSourceNames()) {
            InstanceHandle<FlywayContainer> handle = Arc.container().instance(FlywayContainer.class,
                    getFlywayContainerQualifier(datasourceName));
            if (!handle.isAvailable()) {
                continue;
            }
            result.add(handle.get());
        }
        return result;
    }

    public static Annotation getFlywayContainerQualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        }

        return FlywayDataSource.FlywayDataSourceLiteral.of(dataSourceName);
    }
}
