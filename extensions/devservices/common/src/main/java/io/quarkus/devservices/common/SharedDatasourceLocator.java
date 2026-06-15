package io.quarkus.devservices.common;

import java.util.Optional;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider.RunningDevServicesDatasource;
import io.quarkus.runtime.LaunchMode;

public final class SharedDatasourceLocator {

    private SharedDatasourceLocator() {
    }

    public static Optional<RunningDevServicesDatasource> locate(ContainerLocator containerLocator,
            LaunchMode launchMode, DevServicesDatasourceContainerConfig containerConfig,
            DatasourceServiceConfigurator configurator) {
        if (!containerConfig.isShared()) {
            return Optional.empty();
        }
        return containerLocator
                .locateContainer(containerConfig.getServiceName(), true, launchMode)
                .map(address -> configurator.composeRunningService(address, containerConfig));
    }

    public static <T extends GenericContainer<T>> T configureSharedLabel(T container, LaunchMode launchMode,
            String devServiceLabel, DevServicesDatasourceContainerConfig containerConfig) {
        if (containerConfig.isShared()) {
            return ConfigureUtil.configureSharedServiceLabel(container, launchMode, devServiceLabel,
                    containerConfig.getServiceName());
        }
        return container;
    }
}
