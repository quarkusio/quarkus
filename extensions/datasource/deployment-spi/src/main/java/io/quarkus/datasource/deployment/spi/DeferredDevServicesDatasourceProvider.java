package io.quarkus.datasource.deployment.spi;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.runtime.LaunchMode;

public interface DeferredDevServicesDatasourceProvider extends GenericDevServicesDatasourceProvider {

    DevServicesResultBuildItem.OwnedServiceBuilder<DatasourceStartable> createDatabaseBuilder(Optional<String> username,
            Optional<String> password,
            String datasourceName,
            DevServicesDatasourceContainerConfig devServicesDatasourceContainerConfig,
            LaunchMode launchMode, boolean useSharedNetwork,
            Optional<Duration> startupTimeout);

    Optional<BuilderAndDatasource> getComposeBuilder(LaunchMode launchMode,
            boolean useSharedNetwork, DevServicesDatasourceContainerConfig containerConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem);

    default boolean isDockerRequired() {
        return true;
    }

    record BuilderAndDatasource(DevServicesResultBuildItem.DiscoveredServiceBuilder builder,
            DevServicesDatasourceProvider.RunningDevServicesDatasource datasource) {
    }
}
