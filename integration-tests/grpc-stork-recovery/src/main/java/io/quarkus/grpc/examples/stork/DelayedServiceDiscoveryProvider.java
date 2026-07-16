package io.quarkus.grpc.examples.stork;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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

/**
 * A Stork service discovery that reports no instances until {@code available-after-ms}
 * has elapsed since its first lookup, then reports the configured address.
 * <p>
 * It simulates a gRPC server that is started after the client - the scenario that
 * exposed the {@code stork://} client recovery bugs in both the classic and the
 * Vert.x gRPC clients.
 */
@ServiceDiscoveryType("delayed")
@ServiceDiscoveryAttribute(name = "address-list", description = "comma-separated host:port list served once the delay elapses")
@ServiceDiscoveryAttribute(name = "available-after-ms",
        description = "milliseconds after the first lookup before instances become available")
public class DelayedServiceDiscoveryProvider implements ServiceDiscoveryProvider<DelayedConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(DelayedConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new DelayedServiceDiscovery(config, serviceName);
    }

    private static final class DelayedServiceDiscovery implements ServiceDiscovery {

        private final List<ServiceInstance> instances;
        private final long availableAfterMs;
        // -1 until the first lookup; the delay window is measured from that point so the
        // very first resolution always observes an empty instance list.
        private final AtomicLong firstLookupAt = new AtomicLong(-1);

        DelayedServiceDiscovery(DelayedConfiguration config, String serviceName) {
            String availableAfter = config.getAvailableAfterMs();
            this.availableAfterMs = availableAfter == null ? 2000L : Long.parseLong(availableAfter.trim());
            // Build the instances once so their ids stay stable across lookups - gRPC
            // requires service instance ids to be immutable.
            List<ServiceInstance> list = new ArrayList<>();
            for (String address : config.getAddressList().split(",")) {
                HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address.trim(), 80, serviceName);
                list.add(new DefaultServiceInstance(ServiceInstanceIds.next(), hostAndPort.host, hostAndPort.port, false));
            }
            this.instances = List.copyOf(list);
        }

        @Override
        public Uni<List<ServiceInstance>> getServiceInstances() {
            long now = System.currentTimeMillis();
            firstLookupAt.compareAndSet(-1, now);
            if (now - firstLookupAt.get() < availableAfterMs) {
                // The service has not "started" yet - report no instances.
                return Uni.createFrom().item(List.of());
            }
            return Uni.createFrom().item(instances);
        }
    }
}
