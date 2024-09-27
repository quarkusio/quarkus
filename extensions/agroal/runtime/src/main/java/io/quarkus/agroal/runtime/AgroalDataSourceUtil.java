package io.quarkus.agroal.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;

import io.quarkus.agroal.DataSource;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class AgroalDataSourceUtil {
    private AgroalDataSourceUtil() {
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
