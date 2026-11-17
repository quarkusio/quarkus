package io.quarkus.stork;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.Config;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class SmallRyeStorkRegistrationRecorder {

    private static final Logger LOGGER = Logger.getLogger(SmallRyeStorkRegistrationRecorder.class.getName());

    private final RuntimeValue<StorkConfiguration> runtimeConfig;

    public SmallRyeStorkRegistrationRecorder(final RuntimeValue<StorkConfiguration> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void registerServiceInstance() {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(runtimeConfig.getValue());
        Duration timeout = runtimeConfig.getValue().registrationTimeout();
        Config quarkusConfig = Config.get();
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
                Uni<Void> registration = Stork.getInstance().getService(serviceName)
                        .registerInstance(serviceName, host, port);
                awaitOrSubscribe(registration, serviceName, "registration", timeout);
            }

        }
    }

    public void deregisterServiceInstance(ShutdownContext shutdown) {
        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                deregisterServiceInstance(runtimeConfig.getValue());
            }
        });
    }

    /**
     * Waits for the given {@link Uni} to complete if the caller thread can be blocked,
     * otherwise subscribes and logs on failure. In both cases, the given timeout is applied to
     * avoid hanging indefinitely if the registrar is unreachable.
     *
     * @param uni the operation to execute
     * @param serviceName the service name, used for logging
     * @param action a description of the action (e.g. "registration", "deregistration"), used for logging
     * @param timeout the maximum time to wait for the operation to complete
     */
    private void awaitOrSubscribe(Uni<Void> uni, String serviceName, String action, Duration timeout) {
        if (Infrastructure.canCallerThreadBeBlocked()) {
            try {
                uni.await().atMost(timeout);
                LOGGER.debugf("'%s' successfully completed for service '%s'", action, serviceName);
            } catch (Exception failure) {
                logFailure(failure, serviceName, action, timeout);
            }
        } else {
            uni.ifNoItem().after(timeout).fail()
                    .subscribe().with(
                            success -> LOGGER.debugf("'%s' successfully completed for service '%s'", action, serviceName),
                            failure -> logFailure(failure, serviceName, action, timeout));
        }
    }

    private static void logFailure(Throwable failure, String serviceName, String action, Duration timeout) {
        if (failure instanceof TimeoutException) {
            LOGGER.warnf("Failed to complete %s for service '%s' within %d ms. The timeout can be configured with"
                    + " 'quarkus.stork.registration-timeout'", action, serviceName, timeout.toMillis());
        } else {
            LOGGER.warnf("Failed to complete %s for service '%s': %s", action, serviceName, failure.getMessage());
        }
    }

    private void deregisterServiceInstance(StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.serviceName();
            if (configuration.serviceConfiguration().get(serviceName).serviceRegistrar().isPresent()) {
                StorkServiceRegistrarConfiguration storkServiceRegistrarConfiguration = configuration.serviceConfiguration()
                        .get(serviceName).serviceRegistrar().get();
                if (!storkServiceRegistrarConfiguration.enabled()) {
                    continue;
                }
                Uni<Void> deregistration = Stork.getInstance()
                        .getService(serviceName)
                        .deregisterServiceInstance(serviceName);
                awaitOrSubscribe(deregistration, serviceName, "deregistration", configuration.registrationTimeout());
            }
        }
    }
}
