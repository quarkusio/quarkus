package io.quarkus.stork;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.SimpleServiceConfig;

public class StorkConfigUtil {

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
                        serviceConfiguration.serviceRegistrar().get().type(),
                        serviceConfiguration.serviceRegistrar().get().parameters());
                builder.setServiceRegistrar(serviceRegistrarConfig);
            }
            storkServicesConfigs.add(builder.build());
        }
        return storkServicesConfigs;
    }

    public static StorkConfiguration buildDefaultRegistrationConfig(StorkConfiguration configuration, String serviceRegistrarType) {
        Config quarkusConfig = ConfigProvider.getConfig();
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        List<ServiceConfig> registrationConfigs = serviceConfigs.stream()
                .filter(serviceConfig -> serviceConfig.serviceRegistrar() != null).toList();
        if (registrationConfigs.isEmpty()) {
            String serviceName = quarkusConfig.getOptionalValue("quarkus.application.name", String.class)
                    .orElse("auri-application");
            configuration.serviceConfiguration().put(serviceName, new ServiceConfiguration() {
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
                    return Optional.of(new StorkServiceRegistrarConfiguration() {
                        @Override
                        public String type() {
                            return serviceRegistrarType;
                        }

                        @Override
                        public Map<String, String> parameters() {
                            return Map.of();
                        }
                    });
                }
            });
        }
        return configuration;

    }

    public static InetAddress detectAddress() {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces(); nics
                    .hasMoreElements();) {
                NetworkInterface ifc = nics.nextElement();
                if (ifc.isUp()) {
                    //                    this.log.trace("Testing interface: " + ifc.getDisplayName());
                    if (ifc.getIndex() < lowest || result == null) {
                        lowest = ifc.getIndex();
                    } else if (result != null) {
                        continue;
                    }

                    // @formatter:off
//                    if (!ignoreInterface(ifc.getDisplayName())) {
//                        for (Enumeration<InetAddress> addrs = ifc
//                                .getInetAddresses(); addrs.hasMoreElements();) {
//                            InetAddress address = addrs.nextElement();
//                            if (address instanceof Inet4Address
//                                    && !address.isLoopbackAddress()
//                                    && isPreferredAddress(address)) {
////                                this.log.trace("Found non-loopback interface: "
////                                        + ifc.getDisplayName());
//                                result = address;
//                            }
//                        }
//                    }
                    // @formatter:on
                }
            }
        } catch (IOException ex) {
            //            this.log.error("Cannot get first non-loopback address", ex);
        }

        if (result != null) {
            return result;
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            //            this.log.warn("Unable to retrieve localhost");
        }

        return null;
    }

}
