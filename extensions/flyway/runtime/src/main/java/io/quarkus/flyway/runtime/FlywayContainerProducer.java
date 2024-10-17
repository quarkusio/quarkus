package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.flyway.FlywayDataSource;

public class FlywayContainerProducer implements ContainerProducer {

    private final FlywayRuntimeConfig flywayRuntimeConfig;
    private final FlywayBuildTimeConfig flywayBuildConfig;

    private final List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances;

    FlywayContainerProducer(FlywayRuntimeConfig flywayRuntimeConfig, FlywayBuildTimeConfig flywayBuildConfig,
            @All List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances) {

        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildConfig = flywayBuildConfig;
        this.configCustomizerInstances = configCustomizerInstances;
    }

    @Override
    public String getTenantId(SyntheticCreationalContext<?> context) {
        throw new RuntimeException("Multitenancy is not enabled");
    }

    @Override
    public Annotation getFlywayContainerQualifier(String name) {
        return FlywayContainerUtil.getFlywayContainerQualifier(name);
    }

    @Override
    public FlywayDataSourceRuntimeConfig getRuntimeConfig(String name) {
        return flywayRuntimeConfig.getConfigForDataSourceName(name);
    }

    @Override
    public FlywayDataSourceBuildTimeConfig getBuildTimeConfig(String name) {
        return flywayBuildConfig.getConfigForDataSourceName(name);
    }

    @Override
    public List<FlywayConfigurationCustomizer> matchingConfigCustomizers(String dataSourceName) {
        if ((configCustomizerInstances == null) || configCustomizerInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlywayConfigurationCustomizer> result = new ArrayList<>();
        for (InstanceHandle<FlywayConfigurationCustomizer> instance : configCustomizerInstances) {
            Set<Annotation> qualifiers = instance.getBean().getQualifiers();
            boolean qualifierMatchesDS = false;
            boolean hasFlywayDataSourceQualifier = false;
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(FlywayDataSource.class)) {
                    hasFlywayDataSourceQualifier = true;
                    if (dataSourceName.equals(((FlywayDataSource) qualifier).value())) {
                        qualifierMatchesDS = true;
                        break;
                    }
                }
            }
            if (qualifierMatchesDS) {
                result.add(instance.get());
            } else if (DataSourceUtil.isDefault(dataSourceName) && !hasFlywayDataSourceQualifier) {
                // this is the case where a FlywayConfigurationCustomizer does not have a qualifier at all, therefore is applies to the default datasource
                result.add(instance.get());
            }
        }
        return result;
    }
}
