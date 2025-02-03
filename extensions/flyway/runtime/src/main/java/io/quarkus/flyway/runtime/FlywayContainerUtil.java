package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Default;

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.Arc;
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
        Set<String> activeDs = AgroalDataSourceUtil.activeDataSourceNames();
        Arc.container().select(FlywayContainer.class).stream()
                .filter(container -> activeDs.contains(container.getDataSourceName()))
                .forEach(result::add);
        return result;
    }

    public static Annotation getFlywayContainerQualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        }

        return FlywayDataSource.FlywayDataSourceLiteral.of(dataSourceName);
    }
}
