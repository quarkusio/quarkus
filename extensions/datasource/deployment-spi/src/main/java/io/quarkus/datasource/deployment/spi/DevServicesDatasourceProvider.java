package io.quarkus.datasource.deployment.spi;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.runtime.LaunchMode;

public interface DevServicesDatasourceProvider {

    /**
     * Ecosystem implementations may not have access to a Feature object, so we return a String.
     *
     * @return the result of feature.getName(), or a String representing the feature name
     */
    String getFeature();

    /**
     * The DatasourceStartable should *not* have had `start()` called.
     */
    DatasourceStartable createDatasourceStartable(Optional<String> username,
            Optional<String> password,
            String datasourceName,
            DevServicesDatasourceContainerConfig devServicesDatasourceContainerConfig,
            LaunchMode launchMode, boolean useSharedNetwork,
            Optional<Duration> startupTimeout);

    Optional<DevServicesDatasourceProvider.RunningDevServicesDatasource> findRunningComposeDatasource(
            LaunchMode launchMode,
            boolean useSharedNetwork, DevServicesDatasourceContainerConfig containerConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem);

    default boolean isDockerRequired() {
        return true;
    }

    record RunningDevServicesDatasource(String id, String jdbcUrl, String reactiveUrl, String username, String password) {

    }
}
