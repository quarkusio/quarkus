package io.quarkus.flyway.multitenant.deployment;

import static io.quarkus.datasource.common.runtime.DataSourceUtil.DEFAULT_DATASOURCE_NAME;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.flyway.deployment.FlywayBuildItem;
import io.quarkus.flyway.multitenant.runtime.FlywayMultiTenantBuildTimeConfig;
import io.quarkus.flyway.multitenant.runtime.FlywayMultiTenantContainerProducer;
import io.quarkus.flyway.multitenant.runtime.FlywayPersistenceUnit;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

@BuildSteps(onlyIf = FlywayMultiTenantEnabled.class)
class FlywayMultiTenantProcessor {

    private static final String FLYWAY_CONTAINER_BEAN_NAME_PREFIX = "flyway_multitenant_container_";
    private static final String FLYWAY_BEAN_NAME_PREFIX = "flyway_multitenant_";

    private static final Logger LOGGER = Logger.getLogger(FlywayMultiTenantProcessor.class);

    @BuildStep
    void prepare(BuildProducer<FlywayBuildItem> flywayBuildItems, List<PersistenceUnitDescriptorBuildItem> persistenceUnits,
            FlywayMultiTenantBuildTimeConfig flywayBuildTimeConfig) {
        for (PersistenceUnitDescriptorBuildItem persistenceUnit : persistenceUnits) {
            String persistenceUnitName = persistenceUnit.getPersistenceUnitName();
            String dataSourceName = persistenceUnit.getConfig().getDataSource().orElse(DEFAULT_DATASOURCE_NAME);
            boolean multiTenancyEnabled = persistenceUnit.getConfig().getMultiTenancyStrategy() == MultiTenancyStrategy.SCHEMA;
            FlywayDataSourceBuildTimeConfig buildTimeConfig = flywayBuildTimeConfig
                    .getConfigForPersistenceUnitName(persistenceUnitName);
            AnnotationInstance qualifier = AnnotationInstance.builder(FlywayPersistenceUnit.class)
                    .add("value", persistenceUnitName)
                    .build();
            String flywayBeanName = FLYWAY_BEAN_NAME_PREFIX + persistenceUnitName;
            String containerBeanName = FLYWAY_CONTAINER_BEAN_NAME_PREFIX + persistenceUnitName;
            flywayBuildItems
                    .produce(new FlywayBuildItem(persistenceUnitName, dataSourceName, multiTenancyEnabled, buildTimeConfig,
                            qualifier, flywayBeanName, containerBeanName, 5, FlywayMultiTenantContainerProducer.class));
        }
    }

    @BuildStep
    void createBeans(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // make a FlywayMultiTenantContainerProducer bean
        additionalBeans.produce(
                AdditionalBeanBuildItem.builder().addBeanClasses(FlywayMultiTenantContainerProducer.class).setUnremovable()
                        .setDefaultScope(DotNames.SINGLETON).build());
        // add the @FlywayPersistenceUnit class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(FlywayPersistenceUnit.class).build());
    }
}
