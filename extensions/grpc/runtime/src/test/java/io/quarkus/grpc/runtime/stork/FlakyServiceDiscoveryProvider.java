package io.quarkus.grpc.runtime.stork;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.StorkAddressUtils;

/**
 * Test {@link ServiceDiscovery} that fails on the first lookup and then returns
 * the configured static address list.
 */
final class FlakyServiceDiscoveryProvider implements ServiceDiscoveryProvider<FlakyServiceDiscoveryConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(FlakyServiceDiscoveryConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(config.getAddressList(), 80, serviceName);
        ServiceInstance instance = new DefaultServiceInstance(ServiceInstanceIds.next(), hostAndPort.host,
                hostAndPort.port, false);
        return new FlakyServiceDiscovery(List.of(instance), config.getFailureDelayMs(), config.isEmptyOnFirstLookup());
    }

    private static final class FlakyServiceDiscovery implements ServiceDiscovery {

        private final List<ServiceInstance> instances;
        private final long failureDelayMs;
        private final boolean emptyOnFirstLookup;
        private final AtomicInteger lookups = new AtomicInteger();

        FlakyServiceDiscovery(List<ServiceInstance> instances, long failureDelayMs, boolean emptyOnFirstLookup) {
            this.instances = instances;
            this.failureDelayMs = failureDelayMs;
            this.emptyOnFirstLookup = emptyOnFirstLookup;
        }

        @Override
        public Uni<List<ServiceInstance>> getServiceInstances() {
            if (lookups.getAndIncrement() == 0) {
                if (emptyOnFirstLookup) {
                    return delayedEmptyList();
                }
                Uni<List<ServiceInstance>> failure = Uni.createFrom()
                        .failure(new IllegalStateException("simulated discovery failure"));
                if (failureDelayMs > 0) {
                    return Uni.createFrom().voidItem()
                            .onItem().delayIt().by(Duration.ofMillis(failureDelayMs))
                            .onItem().transformToUni(ignored -> failure);
                }
                return failure;
            }
            return Uni.createFrom().item(instances);
        }

        private Uni<List<ServiceInstance>> delayedEmptyList() {
            if (failureDelayMs > 0) {
                return Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofMillis(failureDelayMs))
                        .onItem().transform(ignored -> List.<ServiceInstance> of());
            }
            return Uni.createFrom().item(List.of());
        }
    }
}
