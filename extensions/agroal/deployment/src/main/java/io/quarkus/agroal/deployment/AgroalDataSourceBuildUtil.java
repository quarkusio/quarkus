package io.quarkus.agroal.deployment;

import java.util.Set;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class AgroalDataSourceBuildUtil {

    public static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());
    public static final DotName AGROAL_DATA_SOURCE = DotName.createSimple(AgroalDataSource.class.getName());
    public static final Set<DotName> AGROAL_INJECTABLE_TYPES = Set.of(DATA_SOURCE, AGROAL_DATA_SOURCE);
    public static final DotName DATASOURCE_QUALIFIER = DotName.createSimple(DataSource.class);

    private AgroalDataSourceBuildUtil() {
    }

    public static AnnotationInstance qualifier(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(DATASOURCE_QUALIFIER).value(dataSourceName).build();
        }
    }

    public static AnnotationInstance[] qualifiers(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return new AnnotationInstance[] { AnnotationInstance.builder(Default.class).build() };
        } else {
            return new AnnotationInstance[] {
                    AnnotationInstance.builder(DotNames.NAMED).value(dataSourceName).build(),
                    AnnotationInstance.builder(DATASOURCE_QUALIFIER).value(dataSourceName).build(),
            };
        }
    }
}
