package io.quarkus.stork;

import java.util.*;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class StorkRegistrarConfigRecorder {

    private static final Logger LOGGER = Logger.getLogger(StorkRegistrarConfigRecorder.class.getName());

    private final RuntimeValue<StorkConfiguration> runtimeConfig;

    public StorkRegistrarConfigRecorder(final RuntimeValue<StorkConfiguration> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setupServiceRegistrarConfig(String serviceRegistrarType, String healthCheckUrl) {
        StorkConfigUtil.requireRegistrarTypeNotBlank(serviceRegistrarType);
        Config quarkusConfig = ConfigProvider.getConfig();
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(runtimeConfig.getValue());
        List<ServiceConfig> registrationConfigs = serviceConfigs.stream()
                .filter(serviceConfig -> serviceConfig.serviceRegistrar() != null).toList();
        String serviceName = quarkusConfig.getValue("quarkus.application.name", String.class);
        if (registrationConfigs.isEmpty()) {
            runtimeConfig.getValue().serviceConfiguration().put(serviceName,
                    StorkConfigUtil.buildDefaultRegistrarConfiguration(serviceRegistrarType, healthCheckUrl));
        } else if (registrationConfigs.size() == 1) {
            serviceName = registrationConfigs.get(0).serviceName();
            runtimeConfig.getValue().serviceConfiguration().computeIfPresent(serviceName,
                    (k, serviceConfiguration) -> StorkConfigUtil.addRegistrarTypeIfAbsent(serviceRegistrarType,
                            serviceConfiguration, healthCheckUrl));
        } else {
            failOnMissingRegistrarTypesForMultipleRegistrars(registrationConfigs);
        }
    }

    private static void failOnMissingRegistrarTypesForMultipleRegistrars(List<ServiceConfig> registrationConfigs) {
        List<String> servicesWithMissingType = new ArrayList<>();
        for (ServiceConfig registrationConfig : registrationConfigs) {
            if (registrationConfig.serviceRegistrar().type().isBlank()) {
                servicesWithMissingType.add(registrationConfig.serviceName());
                LOGGER.info("Missing 'type' for service '" + registrationConfig.serviceName()
                        + "'. This may lead to a runtime error.");
            }

        }
        if (!servicesWithMissingType.isEmpty()) {
            throw new IllegalArgumentException(
                    "Impossible to register service. Missing required 'type' for the following services: " +
                            String.join(", ", servicesWithMissingType));
        }
    }

}
