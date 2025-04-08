package io.quarkus.hibernate.orm.deployment.dev;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = HibernateOrmEnabled.class, onlyIfNot = IsNormal.class)
public class HibernateOrmDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(HibernateOrmDevServicesProcessor.class);

    @BuildStep
    void devServicesAutoGenerateByDefault(List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItems,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            HibernateOrmConfig config,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfigProducer) {
        Set<String> managedSources = schemaReadyBuildItems.stream().map(JdbcDataSourceSchemaReadyBuildItem::getDatasourceNames)
                .collect(HashSet::new, Collection::addAll, Collection::addAll);

        for (Map.Entry<String, HibernateOrmConfigPersistenceUnit> entry : config.persistenceUnits()
                .entrySet()) {
            Optional<String> dataSourceName = entry.getValue().datasource();
            List<String> propertyKeysIndicatingDataSourceConfigured = DataSourceUtil
                    .dataSourcePropertyKeys(dataSourceName.orElse(null), "username");

            if (!managedSources.contains(dataSourceName.orElse(DataSourceUtil.DEFAULT_DATASOURCE_NAME))) {
                String schemaManagementStrategyPropertyKey = HibernateOrmRuntimeConfig.puPropertyKey(entry.getKey(),
                        "schema-management.strategy");
                String legacyDatabaseGenerationPropertyKey = HibernateOrmRuntimeConfig.puPropertyKey(entry.getKey(),
                        "database.generation");
                if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingDataSourceConfigured)
                        && !ConfigUtils.isPropertyNonEmpty(schemaManagementStrategyPropertyKey)
                        && !ConfigUtils.isPropertyNonEmpty(legacyDatabaseGenerationPropertyKey)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                // Only force DB generation if the datasource is configured through dev services
                                if (propertyKeysIndicatingDataSourceConfigured.stream()
                                        .anyMatch(devServicesConfig::containsKey)) {
                                    String forcedValue = "drop-and-create";
                                    LOG.infof("Setting %s=%s to initialize Dev Services managed database",
                                            schemaManagementStrategyPropertyKey, forcedValue);
                                    return Map.of(schemaManagementStrategyPropertyKey, forcedValue);
                                } else {
                                    return Map.of();
                                }
                            }));
                }
            }
        }
    }

}
