package io.quarkus.reactive.datasource.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.sqlclient.Pool;

public final class ReactiveDataSourceUtil {
    private ReactiveDataSourceUtil() {
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
