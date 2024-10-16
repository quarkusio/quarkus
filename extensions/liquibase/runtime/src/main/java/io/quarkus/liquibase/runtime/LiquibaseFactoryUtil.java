package io.quarkus.liquibase.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Default;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;

public final class LiquibaseFactoryUtil {
    private LiquibaseFactoryUtil() {
    }

    public static InstanceHandle<LiquibaseFactory> getLiquibaseFactory(String dataSourceName) {
        return Arc.container().instance(LiquibaseFactory.class,
                getLiquibaseFactoryQualifier(dataSourceName));
    }

    public static List<InstanceHandle<LiquibaseFactory>> getActiveLiquibaseFactories() {
        List<InstanceHandle<LiquibaseFactory>> result = new ArrayList<>();
        for (String datasourceName : Arc.container().instance(DataSources.class).get().getActiveDataSourceNames()) {
            InstanceHandle<LiquibaseFactory> handle = Arc.container().instance(LiquibaseFactory.class,
                    getLiquibaseFactoryQualifier(datasourceName));
            if (!handle.isAvailable()) {
                continue;
            }
            result.add(handle);
        }
        return result;
    }

    public static Annotation getLiquibaseFactoryQualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        }

        return LiquibaseDataSource.LiquibaseDataSourceLiteral.of(dataSourceName);
    }
}
