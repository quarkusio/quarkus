package io.quarkus.stork;

import java.util.*;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class StorkRegistrarConfigRecorder {

    private static final Logger LOGGER = Logger.getLogger(StorkRegistrarConfigRecorder.class.getName());

    public void setupServiceRegistrarConfig(StorkConfiguration config, String serviceRegistrarType) {
        Config quarkusConfig = ConfigProvider.getConfig();
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(config);
        List<ServiceConfig> registrationConfigs = serviceConfigs.stream()
                .filter(serviceConfig -> serviceConfig.serviceRegistrar() != null).toList();
        String serviceName = quarkusConfig.getOptionalValue("quarkus.application.name", String.class)
                .orElse("auri-application");
        if (registrationConfigs.isEmpty()) {
            config.serviceConfiguration().put(serviceName,
                    StorkConfigUtil.buildDefaultRegistrarConfiguration(serviceRegistrarType));
        } else if (registrationConfigs.size() == 1) {
            config.serviceConfiguration().computeIfPresent(serviceName,
                    (k, v) -> StorkConfigUtil.addRegistrarTypeIfAbsent(serviceRegistrarType, v));
        } else {
            failOnMissingRegistrarTypes(registrationConfigs, serviceName);
        }
    }

    private static void failOnMissingRegistrarTypes(List<ServiceConfig> registrationConfigs, String serviceName) {
        List<String> servicesWithMissingType = new ArrayList<>();
        for (ServiceConfig registrationConfig : registrationConfigs) {
            if (registrationConfig.serviceRegistrar().type().isBlank()) {
                servicesWithMissingType.add(registrationConfig.serviceName());
                LOGGER.info("Missing 'type' for service '" + serviceName + "'. This may lead to a runtime error.");
            }

        }
        if (!servicesWithMissingType.isEmpty()) {
            throw new IllegalArgumentException("Missing required 'type' for the following services: " +
                    String.join(", ", servicesWithMissingType));
        }
    }

}
