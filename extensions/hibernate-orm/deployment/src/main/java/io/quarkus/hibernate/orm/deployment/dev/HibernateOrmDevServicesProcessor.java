package io.quarkus.hibernate.orm.deployment.dev;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceFeatureRequirementBuildItem;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spatial.HibernateSpatialAvailable;
import io.quarkus.hibernate.orm.deployment.vector.HibernateVectorAvailable;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, HibernateOrmEnabled.class })
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
                List<String> schemaManagementStrategyPropertyKeys = HibernateOrmRuntimeConfig.puPropertyKeys(entry.getKey(),
                        "schema-management.strategy");
                List<String> legacyDatabaseGenerationPropertyKeys = HibernateOrmRuntimeConfig.puPropertyKeys(entry.getKey(),
                        "database.generation");
                if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingDataSourceConfigured)
                        && !ConfigUtils.isAnyPropertyPresent(schemaManagementStrategyPropertyKeys)
                        && !ConfigUtils.isAnyPropertyPresent(legacyDatabaseGenerationPropertyKeys)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                // Only force DB generation if the datasource is configured through dev services
                                if (propertyKeysIndicatingDataSourceConfigured.stream()
                                        .anyMatch(devServicesConfig::containsKey)) {
                                    List<String> offlineStartKeys = HibernateOrmRuntimeConfig.puPropertyKeys(entry.getKey(),
                                            "database.start-offline");
                                    Optional<Boolean> offlineStart = ConfigUtils
                                            .getFirstOptionalValue(offlineStartKeys, Boolean.class);

                                    if (offlineStart.isEmpty() || !offlineStart.get()) {
                                        String forcedValue = "drop-and-create";
                                        Map<String, String> result = new HashMap<>();
                                        for (String key : schemaManagementStrategyPropertyKeys) {
                                            result.put(key, forcedValue);
                                        }
                                        LOG.infof("Setting %s=%s to initialize Dev Services managed database",
                                                schemaManagementStrategyPropertyKeys, forcedValue);
                                        return result;
                                    } else {
                                        return Map.of();
                                    }

                                } else {
                                    return Map.of();
                                }
                            }));
                }
            }
        }
    }

    @BuildStep(onlyIf = HibernateSpatialAvailable.class)
    DataSourceFeatureRequirementBuildItem requireSpatialFeature() {
        return new DataSourceFeatureRequirementBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                DatabaseFeature.SPATIAL);
    }

    @BuildStep(onlyIf = HibernateVectorAvailable.class)
    DataSourceFeatureRequirementBuildItem requireVectorFeature() {
        return new DataSourceFeatureRequirementBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                DatabaseFeature.VECTOR);
    }
}
