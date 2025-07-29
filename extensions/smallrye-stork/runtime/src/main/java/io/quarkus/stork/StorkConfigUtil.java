package io.quarkus.stork;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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

import io.quarkus.runtime.LaunchMode;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.SimpleServiceConfig;

public class StorkConfigUtil {

    private static final Logger LOGGER = Logger.getLogger(StorkConfigUtil.class.getName());
    private static final String HTTPS = "https://";
    private static final String QUARKUS_HTTP_HOST = "quarkus.http.host";
    private static final String LOCALHOST = "localhost";
    private static final String ALL_INTERFACES = "0.0.0.0";

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
        if (healthCheckPath != null && !healthCheckPath.isBlank()) {
            healthCheckPath = HTTPS + getOrDefaultHost(parameters, quarkusConfig) + ":"
                    + getOrDefaultPort(parameters, quarkusConfig) + healthCheckPath;
            parameters.put("health-check-url", healthCheckPath);
        }
        return buildServiceConfigurationWithRegistrar(serviceRegistrarType, true, parameters);
    }

    public static ServiceConfiguration addRegistrarTypeIfAbsent(String serviceRegistrarType,
            ServiceConfiguration serviceConfiguration, String healthCheckUrl) {
        Optional<StorkServiceRegistrarConfiguration> storkServiceRegistrarConfiguration = serviceConfiguration
                .serviceRegistrar();
        Map<String, String> parameters = storkServiceRegistrarConfiguration
                .map(StorkServiceRegistrarConfiguration::parameters)
                .orElse(new HashMap<>());
        if (healthCheckUrl != null && !healthCheckUrl.isBlank()) {
            parameters.put("health-check-url", healthCheckUrl);
        }
        boolean enabled = storkServiceRegistrarConfiguration.map(StorkServiceRegistrarConfiguration::enabled).orElse(true);
        return buildServiceConfigurationWithRegistrar(serviceRegistrarType, enabled, parameters);
    }

    private static ServiceConfiguration buildServiceConfigurationWithRegistrar(String type, boolean enabled,
            Map<String, String> parameters) {
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
                return Optional.of(buildServiceRegistrarConfiguration(type, enabled, parameters));
            }
        };
    }

    private static StorkServiceRegistrarConfiguration buildServiceRegistrarConfiguration(String type, boolean enabled,
            Map<String, String> parameters) {
        return new StorkServiceRegistrarConfiguration() {
            @Override
            public boolean enabled() {
                return enabled;
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
        String defaultHost;
        if (LaunchMode.current().isDevOrTest()) {
            defaultHost = LOCALHOST;
        } else {
            defaultHost = ALL_INTERFACES;
        }
        String host = quarkusConfig.getOptionalValue(QUARKUS_HTTP_HOST, String.class).orElse(defaultHost);
        if (customHost == null || customHost.isEmpty()) {
            InetAddress inetAddress = StorkConfigUtil.detectAddress();
            customHost = inetAddress != null ? inetAddress.getHostAddress() : host;
        }
        return customHost;
    }

    public static int getOrDefaultPort(Map<String, String> parameters, Config quarkusConfig) {
        String customPort = parameters.getOrDefault("port",
                quarkusConfig.getOptionalValue("quarkus.http.port", String.class).orElse("8080"));
        return Integer.parseInt(customPort);
    }

    public static InetAddress detectAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<NetworkInterfaceWrapper> wrappedInterfaces = new ArrayList<>();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                wrappedInterfaces.add(new StorkNetworkInterfaceWrapper(ni));
            }

            InetAddress addr = findFirstValidAddress(wrappedInterfaces);
            if (addr != null) {
                return addr;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to detect IP address", e);
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.error("Fallback to localhost failed", e);
            return null;
        }
    }

    static InetAddress findFirstValidAddress(List<NetworkInterfaceWrapper> interfaces) {
        try {
            for (NetworkInterfaceWrapper iface : interfaces) {
                if (!iface.isUp() || iface.isLoopback())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("Error checking network interfaces", e);
        }

        return null;
    }

}
