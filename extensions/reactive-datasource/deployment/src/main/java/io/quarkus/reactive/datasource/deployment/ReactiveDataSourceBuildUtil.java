package io.quarkus.reactive.datasource.deployment;

import java.util.Set;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;

public final class ReactiveDataSourceBuildUtil {

    public static final Type VERTX_POOL_TYPE = ClassType.create(ReactiveDataSourceDotNames.VERTX_POOL);
    public static final Set<DotName> REACTIVE_INJECTABLE_TYPES = Set.of(ReactiveDataSourceDotNames.VERTX_POOL);
    public static final DotName REACTIVE_DATASOURCE_QUALIFIER = DotName.createSimple(ReactiveDataSource.class);

    private ReactiveDataSourceBuildUtil() {
    }

    public static AnnotationInstance qualifier(String dataSourceName) {
        if (dataSourceName == null || DataSourceUtil.isDefault(dataSourceName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(REACTIVE_DATASOURCE_QUALIFIER).value(dataSourceName).build();
        }
    }

    public static AnnotationInstance[] qualifiers(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return new AnnotationInstance[] { AnnotationInstance.builder(Default.class).build() };
        } else {
            return new AnnotationInstance[] {
                    AnnotationInstance.builder(DotNames.NAMED).value(dataSourceName).build(),
                    AnnotationInstance.builder(REACTIVE_DATASOURCE_QUALIFIER).value(dataSourceName).build(),
            };
        }
    }
}
