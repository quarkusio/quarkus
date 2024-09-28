package io.quarkus.reactive.datasource.deployment;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;

public final class ReactiveDataSourceBuildUtil {
    private ReactiveDataSourceBuildUtil() {
    }

    public static AnnotationInstance qualifier(String dataSourceName) {
        if (dataSourceName == null || DataSourceUtil.isDefault(dataSourceName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(ReactiveDataSource.class).value(dataSourceName).build();
        }
    }

    public static AnnotationInstance[] qualifiers(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return new AnnotationInstance[] { AnnotationInstance.builder(Default.class).build() };
        } else {
            return new AnnotationInstance[] {
                    AnnotationInstance.builder(DotNames.NAMED).value(dataSourceName).build(),
                    AnnotationInstance.builder(ReactiveDataSource.class).value(dataSourceName).build(),
            };
        }
    }
}
