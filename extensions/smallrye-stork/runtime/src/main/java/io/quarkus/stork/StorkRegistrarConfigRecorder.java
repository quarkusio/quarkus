package io.quarkus.stork;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

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

    /**
     * Builds or completes the registration config for the current Quarkus application based on the given registrar type
     * and optional health check URL.
     * <p>
     * This method is designed to improve the developer experience by automatically setting up service
     * registration with minimal configuration. It ensures that service instances are registered with
     * the appropriate backend (e.g., Consul, Eureka) even if the application does not explicitly provide
     * full registrar settings.
     * <p>
     * Behavior:
     * <ul>
     * <li>If no services are explicitly configured with a service registrar, a default configuration
     * is created and registered under the application name.</li>
     * <li>If exactly one service is configured, its configuration is completed with the missing
     * registrar type or health check URL.</li>
     * <li>If multiple services are configured for registration and one or more lack a type,
     * the method fails with an exception to avoid ambiguity.</li>
     * </ul>
     * <p>
     * This method is intended to be used during application startup to ensure that service registration with
     * Stork is correctly configured, particularly for automatic registration with built-in registrars.
     *
     * @param serviceRegistrarType the type of the service registrar (e.g., "consul", "eureka"); must not be blank
     * @param healthCheckUrl optional health check URL to register with the service (can be {@code null})
     * @throws IllegalArgumentException if {@code serviceRegistrarType} is blank
     * @throws RuntimeException if multiple services are configured for registration and any is missing a registrar type
     */
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
            var enabled = Boolean
                    .parseBoolean(registrationConfig.serviceRegistrar().parameters().getOrDefault("enabled", "true"));
            if (enabled && registrationConfig.serviceRegistrar().type().isBlank()) {
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
