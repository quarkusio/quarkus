package io.quarkus.stork;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;

@Recorder
public class SmallRyeStorkRegistrationRecorder {

    public void registerServiceInstance(StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        Config quarkusConfig = ConfigProvider.getConfig();
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.serviceName();
            Map<String, String> parameters = serviceConfig.serviceRegistrar().parameters();
            String host = parameters.containsKey("ip-address") ? parameters.get("ip-address")
                    : quarkusConfig.getValue("quarkus.http.host", String.class);
            int port = parameters.containsKey("port") ? Integer.parseInt(parameters.get("port"))
                    : Integer.parseInt(quarkusConfig.getValue("quarkus.http.port", String.class));
            if (host == null || host.isEmpty()) {
                InetAddress inetAddress = StorkConfigUtil.detectAddress();
                host = inetAddress != null ? inetAddress.getHostAddress() : host;
            }
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
            Stork.getInstance().getService(serviceName).getServiceRegistrar().deregisterServiceInstance(serviceName).await()
                    .indefinitely();
        }
    }
}
