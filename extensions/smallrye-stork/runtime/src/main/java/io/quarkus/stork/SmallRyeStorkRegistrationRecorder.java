package io.quarkus.stork;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class SmallRyeStorkRegistrationRecorder {

    private static final Logger LOGGER = Logger.getLogger(SmallRyeStorkRegistrationRecorder.class.getName());
    private static final Duration REGISTRATION_TIMEOUT = Duration.ofSeconds(10);

    private final RuntimeValue<StorkConfiguration> runtimeConfig;

    public SmallRyeStorkRegistrationRecorder(final RuntimeValue<StorkConfiguration> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void registerServiceInstance() {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(runtimeConfig.getValue());
        Config quarkusConfig = ConfigProvider.getConfig();
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.serviceName();
            if (runtimeConfig.getValue().serviceConfiguration().get(serviceName).serviceRegistrar().isPresent()) {
                StorkServiceRegistrarConfiguration storkServiceRegistrarConfiguration = runtimeConfig.getValue()
                        .serviceConfiguration()
                        .get(serviceName).serviceRegistrar().get();
                if (!storkServiceRegistrarConfiguration.enabled()) {
                    LOGGER.info("Service registering disabled for  '" + serviceName + "'.");
                    continue;
                }
                Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
                String host = StorkConfigUtil.getOrDefaultHost(parameters,
                        quarkusConfig);
                int port = StorkConfigUtil.getOrDefaultPort(parameters, quarkusConfig);
                if (storkServiceRegistrarConfiguration.instanceName().isPresent()) {
                    Uni<Void> registration = Stork.getInstance().getService(serviceName)
                            .registerInstance(storkServiceRegistrarConfiguration.instanceName().get(), host, port);
                    awaitOrSubscribe(registration, serviceName, "registration");
                } else {
                    Uni<Void> registration = Stork.getInstance().getService(serviceName)
                            .registerInstance(host, port);
                    awaitOrSubscribe(registration, serviceName, "registration");
                }
            }

        }
    }

    public void deregisterServiceInstance(ShutdownContext shutdown) {
        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                deregisterServiceInstance();
            }
        });
    }

    /**
     * Waits for the given {@link Uni} to complete if the caller thread can be blocked,
     * otherwise subscribes and logs on failure. In both cases, a timeout is applied to
     * avoid hanging indefinitely if the registrar is unreachable.
     *
     * @param uni the operation to execute
     * @param serviceName the service name, used for logging
     * @param action a description of the action (e.g. "registration", "deregistration"), used for logging
     */
    private void awaitOrSubscribe(Uni<Void> uni, String serviceName, String action) {
        if (Infrastructure.canCallerThreadBeBlocked()) {
            try {
                uni.await().atMost(REGISTRATION_TIMEOUT);
                LOGGER.debugf("'%s' successfully completed for service '%s'", action, serviceName);
            } catch (Exception failure) {
                LOGGER.warnf("Failed to complete %s for service '%s': %s", action, serviceName, failure.getMessage());
            }
        } else {
            uni.ifNoItem().after(REGISTRATION_TIMEOUT).fail()
                    .subscribe().with(
                            success -> LOGGER.debugf("'%s' successfully completed for service '%s'", action, serviceName),
                            failure -> LOGGER.warnf("Failed to complete %s for service '%s': %s", action, serviceName,
                                    failure.getMessage()));
        }
    }

    private void deregisterServiceInstance() {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(runtimeConfig.getValue());
        Config quarkusConfig = ConfigProvider.getConfig();
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.serviceName();
            if (runtimeConfig.getValue().serviceConfiguration().get(serviceName).serviceRegistrar().isEmpty()) {
                continue;
            }
            StorkServiceRegistrarConfiguration storkServiceRegistrarConfiguration = runtimeConfig.getValue()
                    .serviceConfiguration()
                    .get(serviceName).serviceRegistrar().get();
            if (!storkServiceRegistrarConfiguration.enabled()) {
                continue;
            }
            CountDownLatch registrationLatch = new CountDownLatch(1);
            Uni<Void> deregistration;
            if (storkServiceRegistrarConfiguration.instanceName().isPresent()) {
                deregistration = Stork.getInstance()
                        .getService(serviceName)
                        .deregisterServiceInstance(storkServiceRegistrarConfiguration.instanceName().get());
                awaitOrSubscribe(deregistration, serviceName, "deregistration");
            } else {
                Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
                String host = StorkConfigUtil.getOrDefaultHost(parameters, quarkusConfig);
                int port = StorkConfigUtil.getOrDefaultPort(parameters, quarkusConfig);
                deregistration = Stork.getInstance()
                        .getService(serviceName)
                        .deregisterServiceInstance(host, port);
                awaitOrSubscribe(deregistration, serviceName, "deregistration");
            }
            deregistration
                    .subscribe()
                    .with(
                            success -> registrationLatch.countDown(),
                            failure -> {
                                LOGGER.warnf("Failed to deregister service '%s': %s", serviceName, failure.getMessage());
                                registrationLatch.countDown();
                            });
        }
    }
}
