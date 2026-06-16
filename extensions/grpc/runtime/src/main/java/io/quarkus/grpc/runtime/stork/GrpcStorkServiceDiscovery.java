package io.quarkus.grpc.runtime.stork;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import com.google.common.base.Preconditions;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

/**
 * for gRPC, the service instance id must be immutable.
 * Even a change of attributes of a service instance must result in changing the service instance id.
 */
public class GrpcStorkServiceDiscovery extends NameResolverProvider {
    private static final Logger log = Logger.getLogger(GrpcStorkServiceDiscovery.class);
    public static final Attributes.Key<ServiceInstance> SERVICE_INSTANCE = Attributes.Key.create("service-instance");

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 4; // slightly less important than the default 5
    }

    @Override
    public String getDefaultScheme() {
        return Stork.STORK;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!Stork.STORK.equals(targetUri.getScheme())) {
            return null;
        }
        NameResolver.ServiceConfigParser configParser = args.getServiceConfigParser();
        return new NameResolver() {
            Listener2 listener;
            volatile boolean resolving, shutdown;
            ServiceDiscovery serviceDiscovery;
            String serviceName;
            volatile Set<Long> serviceInstanceIds = new HashSet<>();

            @Override
            public String getServiceAuthority() {
                return targetUri.getAuthority();
            }

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public void start(Listener2 listener) {
                Preconditions.checkState(this.listener == null, "already started");
                this.listener = listener;
                serviceName = targetUri.getHost();
                Service service = Stork.getInstance().getService(serviceName);
                if (service == null) {
                    listener.onError(
                            Status.ABORTED.withDescription("No service definition for serviceName " + serviceName + " found."));
                    return;
                }
                serviceDiscovery = service.getServiceDiscovery();
                resolve();
            }

            private void resolve() {
                if (resolving || shutdown) {
                    return;
                }
                resolving = true;
                Uni<List<ServiceInstance>> serviceInstances = serviceDiscovery.getServiceInstances();
                serviceInstances.subscribe().with(this::informListener, error -> {
                    if (shutdown) {
                        resolving = false;
                        return;
                    }
                    log.warnf(error, "Stork service discovery failed for '%s'", serviceName);
                    listener.onError(Status.UNAVAILABLE
                            .withDescription("Service discovery failed for: " + serviceName)
                            .withCause(error));
                    resolving = false;
                });
            }

            @Override
            public void refresh() {
                resolve();
            }

            private boolean areServicesRemoved(List<ServiceInstance> instances) {
                for (ServiceInstance instance : instances) {
                    if (!serviceInstanceIds.contains(instance.getId())) {
                        return true;
                    }
                }
                return false;
            }

            private void informListener(List<ServiceInstance> instances) {
                try {
                    if (shutdown) {
                        return;
                    }
                    if (instances.isEmpty()) {
                        // No instances were resolved for the service. This must always be
                        // reported to the listener as an error - even when the previous
                        // resolution was also empty - because gRPC only re-runs a
                        // NameResolver (refresh() with backoff) after an onResult or
                        // onError callback. Staying silent here leaves the channel wedged
                        // in CONNECTING with no scheduled re-resolution, so it never
                        // recovers once instances appear (e.g. the service is started
                        // after the client).
                        log.debugf("No service instances resolved for service-name '%s'", serviceName);
                        listener.onError(Status.UNAVAILABLE
                                .withDescription("No instances available for service-name: " + serviceName));
                        return;
                    }

                    if (serviceInstanceIds.size() != instances.size() || areServicesRemoved(instances)) {
                        HashSet<Long> serviceInstanceIds = new HashSet<>();
                        for (ServiceInstance instance : instances) {
                            serviceInstanceIds.add(instance.getId());
                        }
                        this.serviceInstanceIds = serviceInstanceIds;

                        // Always notify the listener for a non-empty resolution, even when the
                        // instance set is unchanged. Skipping onResult/onError when nothing
                        // changed can leave a refresh() with no callback and the channel stuck
                        // waiting for re-resolution (same class of bug as the empty-list case).
                        ArrayList<EquivalentAddressGroup> addresses = new ArrayList<>();
                        for (ServiceInstance instance : instances) {
                            List<SocketAddress> socketAddresses = new ArrayList<>();
                            try {
                                for (InetAddress inetAddress : InetAddress.getAllByName(instance.getHost())) {
                                    socketAddresses.add(new InetSocketAddress(inetAddress, instance.getPort()));
                                }
                            } catch (UnknownHostException e) {
                                log.warnf(e, "Ignoring wrong host: '%s' for service name '%s'", instance.getHost(),
                                        serviceName);
                            }

                            if (!socketAddresses.isEmpty()) {
                                Attributes attributes = Attributes.newBuilder()
                                        .set(SERVICE_INSTANCE, instance)
                                        .build();
                                EquivalentAddressGroup addressGroup = new EquivalentAddressGroup(socketAddresses, attributes);
                                addresses.add(addressGroup);
                            }
                        }

                        if (addresses.isEmpty()) {
                            log.error("Failed to determine working socket addresses for service-name: " + serviceName);
                            listener.onError(Status.FAILED_PRECONDITION);
                        } else {
                            ConfigOrError serviceConfig = configParser.parseServiceConfig(mapConfigForServiceName());
                            listener.onResult(ResolutionResult.newBuilder()
                                    .setAddresses(addresses)
                                    .setServiceConfig(serviceConfig)
                                    .build());
                        }
                    }
                } finally {
                    resolving = false;
                }
            }

            private Map<String, List<Map<String, Map<String, String>>>> mapConfigForServiceName() {
                return Map.of("loadBalancingConfig", List.of(
                        Map.of(Stork.STORK, Map.of("service-name", serviceName))));
            }
        };
    }
}
