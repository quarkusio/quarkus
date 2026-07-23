package io.quarkus.grpc.runtime.stork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.stork.StorkConfigUtil;
import io.quarkus.stork.StorkConfiguration;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkObservation;

/**
 * gRPC resolves Stork service instances in its {@link io.grpc.NameResolver} and then selects
 * from that list synchronously in the load balancer. Stork's
 * {@link Service#selectInstanceAndRecordStart(Collection, boolean)} only records service
 * selection metrics, not discovery. Discovery metrics are recorded once per discovery refresh
 * in {@link GrpcStorkServiceDiscovery} (Netty) or when instances are first fetched in
 * {@link StorkGrpcChannel} (Vert.x).
 */
final class StorkServiceObservationSupport {

    private static volatile StorkConfiguration configuration;
    private static final ConcurrentHashMap<String, ObservationMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    private StorkServiceObservationSupport() {
    }

    /**
     * Clears static caches so live reload / app restart does not keep stale configuration
     * or metric metadata from a previous application generation.
     */
    static void clear() {
        configuration = null;
        METADATA_CACHE.clear();
    }

    static void recordResolvedInstances(Service service, Collection<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return;
        }
        String serviceName = service.getServiceName();
        ObservationMetadata metadata = METADATA_CACHE.computeIfAbsent(serviceName, name -> {
            StorkConfiguration config = getConfiguration();
            if (config == null) {
                return new ObservationMetadata("unknown", "round-robin");
            }
            return new ObservationMetadata(
                    StorkConfigUtil.serviceDiscoveryType(config, name),
                    StorkConfigUtil.serviceSelectionType(config, name));
        });
        ObservationCollector collector = service.getObservations();
        StorkObservation observation = collector.create(
                serviceName,
                metadata.discoveryType(),
                metadata.selectionType());
        observation.onServiceDiscoverySuccess(new ArrayList<>(instances));
    }

    private static StorkConfiguration getConfiguration() {
        StorkConfiguration config = configuration;
        if (config != null) {
            return config;
        }
        try {
            Instance<StorkConfiguration> instance = CDI.current().select(StorkConfiguration.class);
            if (!instance.isResolvable()) {
                return null;
            }
            config = instance.get();
            configuration = config;
            return config;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private record ObservationMetadata(String discoveryType, String selectionType) {
    }
}
