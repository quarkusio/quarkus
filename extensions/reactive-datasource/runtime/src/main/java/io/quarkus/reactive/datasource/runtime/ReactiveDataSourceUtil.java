package io.quarkus.reactive.datasource.runtime;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.sqlclient.Pool;

public final class ReactiveDataSourceUtil {
    private ReactiveDataSourceUtil() {
    }

    public static InjectableInstance<Pool> dataSourceInstance(String dataSourceName) {
        return dataSourceInstance(Pool.class, dataSourceName);
    }

    public static <T extends Pool> InjectableInstance<T> dataSourceInstance(Class<T> type, String dataSourceName) {
        return Arc.container().select(type, qualifier(dataSourceName));
    }

    public static Optional<Pool> dataSourceIfActive(String dataSourceName) {
        return dataSourceIfActive(Pool.class, dataSourceName);
    }

    public static <T extends Pool> Optional<T> dataSourceIfActive(Class<T> type, String dataSourceName) {
        var instance = dataSourceInstance(type, dataSourceName);
        // We want to call get() and throw an exception if the name points to an undefined datasource.
        if (!instance.isResolvable() || instance.getHandle().getBean().isActive()) {
            return Optional.ofNullable(instance.get());
        } else {
            return Optional.empty();
        }
    }

    public static Set<String> activeDataSourceNames() {
        Set<String> activeNames = new LinkedHashSet<>();
        for (var handle : Arc.container().select(Pool.class).handles()) {
            var bean = handle.getBean();
            if (bean != null && bean.isActive()) {
                String name = dataSourceName(bean);
                if (name != null) { // There may be custom beans, these will have a null name and will be ignored.
                    activeNames.add(name);
                }
            }
        }
        return activeNames;
    }

    public static String dataSourceName(Bean<? extends Pool> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof ReactiveDataSource) {
                return ((ReactiveDataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }

    public static Annotation qualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        } else {
            return new ReactiveDataSource.ReactiveDataSourceLiteral(dataSourceName);
        }
    }
}
