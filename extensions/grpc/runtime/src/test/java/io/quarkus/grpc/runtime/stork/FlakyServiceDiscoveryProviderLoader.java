package io.quarkus.grpc.runtime.stork;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;

/**
 * ServiceDiscoveryLoader for {@link FlakyServiceDiscoveryProvider}.
 */
public final class FlakyServiceDiscoveryProviderLoader implements ServiceDiscoveryLoader {

    private final FlakyServiceDiscoveryProvider provider = new FlakyServiceDiscoveryProvider();

    @Override
    public String type() {
        return "flaky";
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ConfigWithType config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return provider.createServiceDiscovery(new FlakyServiceDiscoveryConfiguration(config.parameters()), serviceName,
                serviceConfig, storkInfrastructure);
    }
}
