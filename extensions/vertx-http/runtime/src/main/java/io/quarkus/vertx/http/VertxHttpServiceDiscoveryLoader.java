package io.quarkus.vertx.http;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;

public class VertxHttpServiceDiscoveryLoader implements ServiceDiscoveryLoader {
    @Override
    public ServiceDiscovery createServiceDiscovery(ConfigWithType config, String serviceName, ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {
        return new VertxHttpServiceDiscoveryProvider().createServiceDiscovery((VertxHttpServiceConfig) config, serviceName,
                serviceConfig, storkInfrastructure);
    }

    @Override
    public String type() {
        return "vertx-http";
    }
}
