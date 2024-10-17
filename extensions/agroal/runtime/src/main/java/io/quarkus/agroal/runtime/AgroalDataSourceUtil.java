package io.quarkus.agroal.runtime;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class AgroalDataSourceUtil {
    private AgroalDataSourceUtil() {
    }

    public static InjectableInstance<AgroalDataSource> dataSourceInstance(String dataSourceName) {
        return Arc.container().select(AgroalDataSource.class, qualifier(dataSourceName));
    }

    public static Optional<AgroalDataSource> dataSourceIfActive(String dataSourceName) {
        var instance = dataSourceInstance(dataSourceName);
        // We want to call get() and throw an exception if the name points to an undefined datasource.
        if (!instance.isResolvable() || instance.getHandle().getBean().isActive()) {
            return Optional.ofNullable(instance.get());
        } else {
            return Optional.empty();
        }
    }

    public static Set<String> activeDataSourceNames() {
        Set<String> activeNames = new LinkedHashSet<>();
        for (var handle : Arc.container().select(AgroalDataSource.class).handles()) {
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

    public static String dataSourceName(Bean<? extends javax.sql.DataSource> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof DataSource) {
                return ((DataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }

    public static Annotation qualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        } else {
            return new DataSource.DataSourceLiteral(dataSourceName);
        }
    }
}
