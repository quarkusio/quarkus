package io.quarkus.it.rest.client.reactive.stork;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.StorkAddressUtils;
import io.vertx.core.Vertx;

@ServiceDiscoveryType("my")
@ServiceDiscoveryAttribute(name = "secure", description = "https")
@ServiceDiscoveryAttribute(name = "address-list", description = "a comma-separated list of addresses")
public class MyServiceDiscoveryProvider implements ServiceDiscoveryProvider<MyConfiguration> {
    public static volatile Vertx providedVertx;

    @Override
    public ServiceDiscovery createServiceDiscovery(MyConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        providedVertx = storkInfrastructure.get(Vertx.class, () -> null);
        String addressList = config.getAddressList();
        boolean secure = Boolean.parseBoolean(config.getSecure());
        Uni<List<ServiceInstance>> instances = Uni.createFrom()
                .item(Arrays.stream(addressList.split(","))
                        .map(address -> toServiceInstance(address, serviceName, secure))
                        .collect(Collectors.toList()));
        return () -> instances;
    }

    private ServiceInstance toServiceInstance(String address, String serviceName, boolean secure) {
        HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address, 80, serviceName);
        return new DefaultServiceInstance(ServiceInstanceIds.next(), hostAndPort.host, hostAndPort.port, secure);
    }
}
