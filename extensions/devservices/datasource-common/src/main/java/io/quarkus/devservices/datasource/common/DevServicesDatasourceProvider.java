package io.quarkus.devservices.datasource.common;

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

    /**
     * Looks for a running container shared by another application, using label-based service discovery, as
     * configured through {@code devservices.shared}/{@code devservices.service-name}.
     */
    default Optional<DevServicesDatasourceProvider.RunningDevServicesDatasource> findRunningSharedDatasource(
            LaunchMode launchMode, DevServicesDatasourceContainerConfig containerConfig) {
        return Optional.empty();
    }

    default boolean isDockerRequired() {
        return true;
    }

    record RunningDevServicesDatasource(String id, String jdbcUrl, String reactiveUrl, String username, String password) {

    }
}
