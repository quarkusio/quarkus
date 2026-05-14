package io.quarkus.stork;

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
                String instanceName = storkServiceRegistrarConfiguration.instanceName().orElse(null);
                Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
                String host = StorkConfigUtil.getOrDefaultHost(parameters,
                        quarkusConfig);
                int port = StorkConfigUtil.getOrDefaultPort(parameters, quarkusConfig);
                Stork.getInstance().getService(serviceName).registerInstance(serviceName, instanceName, host,
                        port).await().indefinitely();
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
                @SuppressWarnings("unchecked")
                Uni<Void> unchecked = Stork.getInstance()
                        .getService(serviceName)
                        .getServiceRegistrar()
                        .deregisterServiceInstance(serviceName, storkServiceRegistrarConfiguration.instanceName().get());
                deregistration = unchecked;
            } else {
                Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
                String host = StorkConfigUtil.getOrDefaultHost(parameters, quarkusConfig);
                int port = StorkConfigUtil.getOrDefaultPort(parameters, quarkusConfig);
                deregistration = Stork.getInstance()
                        .getService(serviceName)
                        .deregisterServiceInstance(serviceName, host, port);
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
