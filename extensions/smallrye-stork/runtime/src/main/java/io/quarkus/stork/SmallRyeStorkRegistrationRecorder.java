package io.quarkus.stork;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class SmallRyeStorkRegistrationRecorder {

    private static final Logger LOGGER = Logger.getLogger(StorkRegistrarConfigRecorder.class.getName());

    public void registerServiceInstance(StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        Config quarkusConfig = ConfigProvider.getConfig();
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.serviceName();
            if (configuration.serviceConfiguration().get(serviceName).serviceRegistrar().isPresent()) {
                StorkServiceRegistrarConfiguration storkServiceRegistrarConfiguration = configuration.serviceConfiguration()
                        .get(serviceName).serviceRegistrar().get();
                if (!storkServiceRegistrarConfiguration.enabled()) {
                    LOGGER.info("Service registering disabled for  '" + serviceName + "'.");
                    continue;
                }
            }
            Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
            String host = StorkConfigUtil.getOrDefaultHost(parameters,
                    quarkusConfig);
            int port = StorkConfigUtil.getOrDefaultPort(parameters, quarkusConfig);
            Stork.getInstance().getService(serviceName).getServiceRegistrar().registerServiceInstance(serviceName, host,
                    port).await().indefinitely();
        }
    }

    public void deregisterServiceInstance(ShutdownContext shutdown, StorkConfiguration configuration) {
        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                deregisterServiceInstance(configuration);
            }
        });
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
            }
            Stork.getInstance().getService(serviceName).getServiceRegistrar().deregisterServiceInstance(serviceName).await()
                    .indefinitely();
        }
    }
}
