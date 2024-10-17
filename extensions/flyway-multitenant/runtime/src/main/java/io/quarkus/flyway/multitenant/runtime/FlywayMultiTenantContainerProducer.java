package io.quarkus.flyway.multitenant.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.flyway.runtime.ContainerProducer;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayDataSourceRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class FlywayMultiTenantContainerProducer implements ContainerProducer {

    private final FlywayMultiTenantRuntimeConfig flywayMultiTenantRuntimeConfig;
    private final FlywayMultiTenantBuildTimeConfig flywayMultiTenantBuildConfig;

    private final List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances;

    FlywayMultiTenantContainerProducer(FlywayMultiTenantRuntimeConfig flywayMultiTenantRuntimeConfig,
            FlywayMultiTenantBuildTimeConfig flywayMultiTenantBuildConfig,
            @All List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances) {

        this.flywayMultiTenantRuntimeConfig = flywayMultiTenantRuntimeConfig;
        this.flywayMultiTenantBuildConfig = flywayMultiTenantBuildConfig;
        this.configCustomizerInstances = configCustomizerInstances;
    }

    @Override
    public String getTenantId(SyntheticCreationalContext<?> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        FlywayPersistenceUnit annotation = (FlywayPersistenceUnit) injectionPoint.getQualifiers().stream()
                .filter(x -> x instanceof FlywayPersistenceUnit)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "flyway must be qualified with FlywayPersistenceUnit"));
        return annotation.tenantId();
    }

    @Override
    public Annotation getFlywayContainerQualifier(String name) {
        return FlywayMultiTenantContainerUtil.getFlywayContainerQualifier(name);
    }

    @Override
    public FlywayDataSourceRuntimeConfig getRuntimeConfig(String name) {
        return flywayMultiTenantRuntimeConfig.getConfigForPersistenceUnitName(name);
    }

    @Override
    public FlywayDataSourceBuildTimeConfig getBuildTimeConfig(String name) {
        return flywayMultiTenantBuildConfig.getConfigForPersistenceUnitName(name);
    }

    @Override
    public List<FlywayConfigurationCustomizer> matchingConfigCustomizers(String persistenceUnitName) {
        if ((configCustomizerInstances == null) || configCustomizerInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlywayConfigurationCustomizer> result = new ArrayList<>();
        for (InstanceHandle<FlywayConfigurationCustomizer> instance : configCustomizerInstances) {
            Set<Annotation> qualifiers = instance.getBean().getQualifiers();
            boolean qualifierMatchesPS = false;
            boolean hasFlywayPersistenceUnitQualifier = false;
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(FlywayPersistenceUnit.class)) {
                    hasFlywayPersistenceUnitQualifier = true;
                    if (persistenceUnitName.equals(((FlywayPersistenceUnit) qualifier).value())) {
                        qualifierMatchesPS = true;
                        break;
                    }
                }
            }
            if (qualifierMatchesPS) {
                result.add(instance.get());
            } else if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)
                    && !hasFlywayPersistenceUnitQualifier) {
                // this is the case where a FlywayConfigurationCustomizer does not have a qualifier at all, therefore is applies to the default datasource
                result.add(instance.get());
            }
        }
        return result;
    }
}
