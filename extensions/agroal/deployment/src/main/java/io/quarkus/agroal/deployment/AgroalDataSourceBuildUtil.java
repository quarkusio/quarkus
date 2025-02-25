package io.quarkus.agroal.deployment;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.agroal.DataSource;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class AgroalDataSourceBuildUtil {
    private AgroalDataSourceBuildUtil() {
    }

    public static AnnotationInstance qualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(DataSource.class).value(dataSourceName).build();
        }
    }

    public static AnnotationInstance[] qualifiers(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return new AnnotationInstance[] { AnnotationInstance.builder(Default.class).build() };
        } else {
            return new AnnotationInstance[] {
                    AnnotationInstance.builder(DotNames.NAMED).value(dataSourceName).build(),
                    AnnotationInstance.builder(DataSource.class).value(dataSourceName).build(),
            };
        }
    }
}
