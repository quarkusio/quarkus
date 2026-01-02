package io.quarkus.stork;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Builds a default {@link ServiceConfiguration} for a Quarkus application
     * when no explicit service registrar configuration is defined by the developer.
     * <p>
     * This method generates a minimal configuration with the provided registrar type
     * and optionally includes a fully resolved health check URL.
     * It retrieves default host and port values from the current Quarkus configuration
     * if needed to complete the health check URL.
     *
     * @param serviceRegistrarType the type of the registrar (e.g., "consul", "eureka"); must not be blank
     * @param healthCheckPath the relative path to the health check endpoint (e.g., {@code /q/health/live});
     *        if provided, a full URL will be constructed using default host and port
     * @return a {@link ServiceConfiguration} pre-filled with the type and optional health check URL
     * @throws IllegalArgumentException if {@code serviceRegistrarType} is null or blank
     */

    public static ServiceConfiguration buildDefaultRegistrarConfiguration(String serviceRegistrarType, String healthCheckPath) {
        requireRegistrarTypeNotBlank(serviceRegistrarType);
        Map<String, String> parameters = new HashMap<>();
        Config quarkusConfig = ConfigProvider.getConfig();
        if (healthCheckPath != null && !healthCheckPath.isBlank()) {
            healthCheckPath = HTTPS + getOrDefaultHost(parameters, quarkusConfig) + ":"
                    + getOrDefaultPort(parameters, quarkusConfig) + healthCheckPath;
            parameters.put("health-check-url", healthCheckPath);
        }
        return buildServiceConfigurationWithRegistrar(serviceRegistrarType, true, parameters);
    }

    /**
     * Returns a copy of the given {@link ServiceConfiguration} with the registrar type
     * and optional health check URL added if the registrar type is not already set.
     * <p>
     * If the registrar is already present, its configuration is reused and only the missing
     * health check URL may be appended. The registrar is marked as enabled by default unless
     * specified otherwise.
     *
     * @param serviceRegistrarType the registrar type to set if missing (e.g., "consul"); must not be blank
     * @param serviceConfiguration the existing service configuration to update
     * @param healthCheckUrl optional full health check URL to add to the parameters
     * @return an updated {@link ServiceConfiguration} with type and health check URL as needed
     * @throws IllegalArgumentException if {@code serviceRegistrarType} is null or blank
     */
    public static ServiceConfiguration addRegistrarTypeIfAbsent(String serviceRegistrarType,
            ServiceConfiguration serviceConfiguration, String healthCheckUrl) {
        requireRegistrarTypeNotBlank(serviceRegistrarType);
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

    /**
     * Resolves the host (IP address or hostname) to use for service registration.
     * <p>
     * The method follows the following resolution strategy:
     * <ul>
     * <li>If {@code parameters} contains a custom {@code ip-address}, it is returned.</li>
     * <li>Otherwise, in dev/test mode, {@code localhost} is used as fallback.</li>
     * <li>In other modes, {@code 0.0.0.0} is used unless overridden by {@code quarkus.http.host}.</li>
     * <li>Finally, if no configuration is present, the method attempts to detect a valid local IP address.</li>
     * </ul>
     *
     * @param parameters a map of registrar parameters that may include {@code ip-address}
     * @param quarkusConfig the active Quarkus {@link Config} instance
     * @return the resolved host to use for registration
     */
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

    /**
     * Resolves the port to use for service registration.
     * <p>
     * Priority order:
     * <ol>
     * <li>If {@code parameters} includes a {@code port}, it is used.</li>
     * <li>Else, the value from {@code quarkus.http.port} is used, if present.</li>
     * <li>Otherwise, defaults to {@code 8080}.</li>
     * </ol>
     *
     * @param parameters a map of registrar parameters that may include {@code port}
     * @param quarkusConfig the active Quarkus {@link Config} instance
     * @return the resolved port as an integer
     */
    public static int getOrDefaultPort(Map<String, String> parameters, Config quarkusConfig) {
        String customPort = parameters.getOrDefault("port",
                quarkusConfig.getOptionalValue("quarkus.http.port", String.class).orElse("8080"));
        return Integer.parseInt(customPort);
    }

    /**
     * Attempts to detect the first valid non-loopback IPv4 address of the host machine.
     * <p>
     * This is used when no explicit IP address is configured. The method iterates over
     * all available network interfaces, filtering out loopback and down interfaces.
     * <p>
     * If detection fails, it falls back to {@link InetAddress#getLocalHost()}.
     *
     * @return the detected {@link InetAddress}, or {@code null} if none found
     */
    public static InetAddress detectAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress addr = findFirstValidAddress(Collections.list(interfaces));
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

    /**
     * Finds the first valid IPv4 address from the provided list of network interfaces.
     * <p>
     * The method checks that each interface is up and not a loopback, and then looks for
     * a non-loopback {@link Inet4Address} among the available addresses.
     *
     * @param interfaces a list of {@link NetworkInterface} representing system interfaces
     * @return the first valid {@link InetAddress}, or {@code null} if none found
     */
    private static InetAddress findFirstValidAddress(List<NetworkInterface> interfaces) {
        try {
            for (NetworkInterface iface : interfaces) {
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

    public static void requireRegistrarTypeNotBlank(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Parameter type should be provided.");
        }
    }

}
