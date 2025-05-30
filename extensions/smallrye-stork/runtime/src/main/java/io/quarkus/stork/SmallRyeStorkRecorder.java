package io.quarkus.stork;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.vertx.core.Vertx;

@Recorder
public class SmallRyeStorkRecorder {

    public void initialize(ShutdownContext shutdown, RuntimeValue<Vertx> vertx, StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        StorkConfigProvider.init(serviceConfigs);
        Instance<ObservationCollector> instance = CDI.current().select(ObservationCollector.class);
        if (instance.isResolvable()) {
            Stork.initialize(new QuarkusStorkObservableInfrastructure(vertx.getValue(), instance.get()));
        } else {
            QuarkusStorkInfrastructure infrastructure = new QuarkusStorkInfrastructure(vertx.getValue());
            Stork.initialize(infrastructure);
        }

        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }

    public void deregisterServiceInstance(ShutdownContext shutdown, StorkConfiguration configuration) {
        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }

    private void deregisterServiceInstance(StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        List<ServiceConfig> registrationConfigs = serviceConfigs.stream()
                .filter(serviceConfig -> serviceConfig.serviceRegistrar() != null).toList();

        registrationConfigs.get(0).serviceName();
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
}
