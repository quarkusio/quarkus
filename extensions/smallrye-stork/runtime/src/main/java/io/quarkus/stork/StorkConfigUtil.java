package io.quarkus.stork;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.SimpleServiceConfig;

public class StorkConfigUtil {

    private static final Logger LOGGER = Logger.getLogger(StorkConfigUtil.class.getName());
    public static final String HTTPS = "https://";
    public static final String QUARKUS_HTTP_HOST = "quarkus.http.host";

    public static List<ServiceConfig> toStorkServiceConfig(StorkConfiguration storkConfiguration) {
        List<ServiceConfig> storkServicesConfigs = new ArrayList<>();
        Set<String> servicesConfigs = storkConfiguration.serviceConfiguration().keySet();
        SimpleServiceConfig.Builder builder = new SimpleServiceConfig.Builder();
        for (String serviceName : servicesConfigs) {
            builder.setServiceName(serviceName);
            ServiceConfiguration serviceConfiguration = storkConfiguration.serviceConfiguration().get(serviceName);
            if (serviceConfiguration.serviceDiscovery().isPresent()) {
                SimpleServiceConfig.SimpleServiceDiscoveryConfig storkServiceDiscoveryConfig = new SimpleServiceConfig.SimpleServiceDiscoveryConfig(
                        serviceConfiguration.serviceDiscovery().get().type(),
                        serviceConfiguration.serviceDiscovery().get().params());
                builder = builder.setServiceDiscovery(storkServiceDiscoveryConfig);
                SimpleServiceConfig.SimpleLoadBalancerConfig loadBalancerConfig = new SimpleServiceConfig.SimpleLoadBalancerConfig(
                        serviceConfiguration.loadBalancer().type(), serviceConfiguration.loadBalancer().parameters());
                builder.setLoadBalancer(loadBalancerConfig);
            }
            if (serviceConfiguration.serviceRegistrar().isPresent()) {
                SimpleServiceConfig.SimpleServiceRegistrarConfig serviceRegistrarConfig = new SimpleServiceConfig.SimpleServiceRegistrarConfig(
                        serviceConfiguration.serviceRegistrar().get().type().orElse(""),
                        serviceConfiguration.serviceRegistrar().get().parameters());
                builder.setServiceRegistrar(serviceRegistrarConfig);
            }
            storkServicesConfigs.add(builder.build());
        }
        return storkServicesConfigs;
    }

    public static ServiceConfiguration buildDefaultRegistrarConfiguration(String serviceRegistrarType, String healthCheckPath) {
        Map<String, String> parameters = new HashMap<>();
        Config quarkusConfig = ConfigProvider.getConfig();
        String defaultHost = quarkusConfig.getValue(QUARKUS_HTTP_HOST, String.class);
        if (healthCheckPath != null && !healthCheckPath.isBlank()) {
            healthCheckPath = HTTPS + getOrDefaultHost(parameters, quarkusConfig) + ":"
                    + getOrDefaultPort(parameters, quarkusConfig) + healthCheckPath;
            parameters.put("health-check-url", healthCheckPath);
        }
        return buildServiceConfigurationWithRegistrar(serviceRegistrarType, parameters);
    }

    public static ServiceConfiguration addRegistrarTypeIfAbsent(String serviceRegistrarType,
            ServiceConfiguration serviceConfiguration, String healthCheckUrl) {
        Map<String, String> parameters = serviceConfiguration.serviceRegistrar()
                .map(StorkServiceRegistrarConfiguration::parameters)
                .orElse(new HashMap<>());
        if (healthCheckUrl != null && !healthCheckUrl.isBlank()) {
            parameters.put("health-check-url", healthCheckUrl);
        }
        return buildServiceConfigurationWithRegistrar(serviceRegistrarType, parameters);
    }

    private static ServiceConfiguration buildServiceConfigurationWithRegistrar(String type, Map<String, String> parameters) {
        return new ServiceConfiguration() {
            @Override
            public Optional<StorkServiceDiscoveryConfiguration> serviceDiscovery() {
                return Optional.empty();
            }

            @Override
            public StorkLoadBalancerConfiguration loadBalancer() {
                return null;
            }

            @Override
            public Optional<StorkServiceRegistrarConfiguration> serviceRegistrar() {
                return Optional.of(buildServiceRegistrarConfiguration(type, parameters));
            }
        };
    }

    private static StorkServiceRegistrarConfiguration buildServiceRegistrarConfiguration(String type,
            Map<String, String> parameters) {
        return new StorkServiceRegistrarConfiguration() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Optional<String> type() {
                return Optional.of(type);
            }

            @Override
            public Map<String, String> parameters() {
                return parameters;
            }
        };
    }

    public static String getOrDefaultHost(Map<String, String> parameters, Config quarkusConfig) {
        String customHost = parameters.containsKey("ip-address") ? parameters.get("ip-address")
                : null;
        String defaultHost = quarkusConfig.getValue("quarkus.http.host", String.class);
        if (customHost == null || customHost.isEmpty()) {
            InetAddress inetAddress = StorkConfigUtil.detectAddress();
            customHost = inetAddress != null ? inetAddress.getHostAddress() : defaultHost;
        }
        return customHost;
    }

    public static int getOrDefaultPort(Map<String, String> parameters, Config quarkusConfig) {
        String customPort =  parameters.getOrDefault("port", quarkusConfig.getValue("quarkus.http.port", String.class));
        return Integer.parseInt(customPort);
    }

    public static InetAddress detectAddress() {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp()) {
                    LOGGER.debug("Testing interface: {}" + networkInterface.getDisplayName());
                    if (networkInterface.getIndex() < lowest || result == null) {
                        lowest = networkInterface.getIndex();
                    } else if (result != null) {
                        continue;
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Unable to get first non-loopback address", ex);
        }
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.error("Unable to detect address", e);
        }

        return null;
    }

}
