package io.quarkus.hibernate.orm.deployment.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.spatial.HibernateSpatialAvailable;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.vector.HibernateVectorAvailable;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, HibernateOrmEnabled.class })
public class HibernateOrmDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(HibernateOrmDevServicesProcessor.class);

    @BuildStep
    void devServicesAutoGenerateByDefault(List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItems,
            List<PersistenceUnitDefinedBuildItem> definedPersistenceUnits,
            List<HibernateOrmClientDefinedBuildItem> definedClients,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfigProducer) {
        Set<String> managedSources = schemaReadyBuildItems.stream().map(JdbcDataSourceSchemaReadyBuildItem::getDatasourceNames)
                .collect(HashSet::new, Collection::addAll, Collection::addAll);

        Map<String, List<HibernateOrmClientDefinedBuildItem>> clientsByName = new LinkedHashMap<>();
        for (HibernateOrmClientDefinedBuildItem client : definedClients) {
            clientsByName.computeIfAbsent(client.getName(), k -> new ArrayList<>()).add(client);
        }

        for (PersistenceUnitDefinedBuildItem pu : definedPersistenceUnits) {
            String puName = pu.getPersistenceUnitName();
            List<String> schemaManagementStrategyPropertyKeys = HibernateOrmRuntimeConfig.puPropertyKeys(puName,
                    "schema-management.strategy");

            if (pu.getDataSourceName().isEmpty()) {
                boolean clientDevServicesEnabled = pu.getClientName()
                        .map(clientsByName::get)
                        .filter(clients -> clients.size() == 1)
                        .map(clients -> clients.get(0))
                        .map(HibernateOrmClientDefinedBuildItem::isDevServicesEnabled)
                        .orElse(false);
                if (clientDevServicesEnabled
                        && !ConfigUtils.isAnyPropertyPresent(schemaManagementStrategyPropertyKeys)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                String forcedValue = "drop-and-create";
                                Map<String, String> result = new HashMap<>();
                                for (String key : schemaManagementStrategyPropertyKeys) {
                                    result.put(key, forcedValue);
                                }
                                LOG.infof("Setting %s=%s to initialize Dev Services managed database",
                                        schemaManagementStrategyPropertyKeys, forcedValue);
                                return result;
                            }));
                }
                continue;
            }

            String dataSourceName = pu.getDataSourceName().get();
            List<String> propertyKeysIndicatingDataSourceConfigured = DataSourceUtil
                    .dataSourcePropertyKeys(dataSourceName, "username");

            if (!managedSources.contains(dataSourceName)) {
                if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingDataSourceConfigured)
                        && !ConfigUtils.isAnyPropertyPresent(schemaManagementStrategyPropertyKeys)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                // Only force DB generation if the datasource is configured through dev services
                                if (propertyKeysIndicatingDataSourceConfigured.stream()
                                        .anyMatch(devServicesConfig::containsKey)) {
                                    List<String> offlineStartKeys = HibernateOrmRuntimeConfig.puPropertyKeys(puName,
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
