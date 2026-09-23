package io.quarkus.grpc.runtime.stork;

import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_MEASURE_TIME;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_SERVICE_INSTANCE;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.logging.Logger;

import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.JsonUtil;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;

public class GrpcLoadBalancerProvider extends LoadBalancerProvider {
    private static final Logger log = Logger.getLogger(GrpcLoadBalancerProvider.class);

    private final boolean requestConnections;

    /**
     * @param requestConnections if true, the load balancer will proactively request connections from available channels.
     *        This leads to better load balancing at the cost of keeping active connections.
     */
    public GrpcLoadBalancerProvider(boolean requestConnections) {
        this.requestConnections = requestConnections;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 4; // less than the default one, we're using stork only when selected
    }

    @Override
    public String getPolicyName() {
        return Stork.STORK;
    }

    @Override
    public NameResolver.ConfigOrError parseLoadBalancingPolicyConfig(Map<String, ?> rawConfig) {
        String serviceName;
        try {
            serviceName = JsonUtil.getString(rawConfig, "service-name");
        } catch (RuntimeException e) {
            log.error("Failed to parse Stork configuration: " + rawConfig, e);
            return NameResolver.ConfigOrError.fromError(Status.INTERNAL);
        }
        if (serviceName == null) {
            log.error("No 'service-name' defined in the Stork for gRPC configuration: " + rawConfig);
            return NameResolver.ConfigOrError.fromError(Status.INTERNAL);
        }
        return NameResolver.ConfigOrError
                .fromConfig(new StorkLoadBalancerConfig(serviceName));
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
        return new LoadBalancer() {

            final Map<AddressKey, ManagedSubchannel> subchannelsByAddress = new LinkedHashMap<>();
            final Map<ServiceInstance, Subchannel> subchannelsByServiceInstance = new TreeMap<>(
                    Comparator.comparingLong(ServiceInstance::getId));
            final Set<ServiceInstance> activeServiceInstances = new HashSet<>();

            String serviceName;

            @Override
            public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
                List<EquivalentAddressGroup> addresses = resolvedAddresses.getAddresses();

                Object loadBalancerConfig = resolvedAddresses.getLoadBalancingPolicyConfig();
                if (!(loadBalancerConfig instanceof StorkLoadBalancerConfig)) {
                    throw new IllegalStateException("invalid configuration for a Stork Load Balancer : " + loadBalancerConfig);
                }

                StorkLoadBalancerConfig config = (StorkLoadBalancerConfig) loadBalancerConfig;

                serviceName = config.serviceName;

                Map<AddressKey, EquivalentAddressGroup> desiredAddresses = new LinkedHashMap<>();
                Map<ServiceInstance, Subchannel> desiredSubchannelsByServiceInstance = new TreeMap<>(
                        Comparator.comparingLong(ServiceInstance::getId));

                for (EquivalentAddressGroup addressGroup : addresses) {
                    ServiceInstance serviceInstance = addressGroup.getAttributes()
                            .get(GrpcStorkServiceDiscovery.SERVICE_INSTANCE);
                    if (serviceInstance == null) {
                        log.warn("Ignoring gRPC Stork address group without a service instance");
                        continue;
                    }
                    AddressKey addressKey = AddressKey.from(addressGroup);
                    desiredAddresses.put(addressKey, addressGroup);
                    ManagedSubchannel managedSubchannel = subchannelsByAddress.get(addressKey);
                    if (managedSubchannel == null) {
                        managedSubchannel = createManagedSubchannel(addressKey, addressGroup, serviceInstance, helper);
                        subchannelsByAddress.put(addressKey, managedSubchannel);
                    } else {
                        managedSubchannel.update(addressGroup, serviceInstance);
                    }
                    desiredSubchannelsByServiceInstance.put(serviceInstance, managedSubchannel.subchannel);
                }

                shutdownRemovedSubchannels(desiredAddresses.keySet());
                subchannelsByServiceInstance.clear();
                subchannelsByServiceInstance.putAll(desiredSubchannelsByServiceInstance);
                rebuildActiveServiceInstances();
                updateBalancingState(helper);
            }

            @Override
            public void handleNameResolutionError(Status error) {
                log.errorf("Name resolution failed for service '%s'", serviceName);
                if (activeServiceInstances.isEmpty()) {
                    helper.updateBalancingState(TRANSIENT_FAILURE, new GrpcLoadBalancerProvider.ErrorPicker(error));
                }
            }

            @Override
            public void shutdown() {
                log.debugf("Shutting down load balancer for service '%s'", serviceName);
                for (ManagedSubchannel managedSubchannel : subchannelsByAddress.values()) {
                    managedSubchannel.shutdown();
                }
                subchannelsByAddress.clear();
                subchannelsByServiceInstance.clear();
                activeServiceInstances.clear();
            }

            private ManagedSubchannel createManagedSubchannel(AddressKey addressKey, EquivalentAddressGroup addressGroup,
                    ServiceInstance serviceInstance, LoadBalancer.Helper helper) {
                CreateSubchannelArgs subChannelArgs = CreateSubchannelArgs.newBuilder()
                        .setAddresses(addressGroup)
                        .setAttributes(addressGroup.getAttributes())
                        .build();

                Subchannel subchannel = helper.createSubchannel(subChannelArgs);
                ManagedSubchannel managedSubchannel = new ManagedSubchannel(addressKey, subchannel, addressGroup,
                        serviceInstance);
                subchannel.start(new SubchannelStateListener() {
                    @Override
                    public void onSubchannelState(ConnectivityStateInfo stateInfo) {
                        handleSubchannelState(managedSubchannel, stateInfo, helper);
                    }
                });
                if (requestConnections) {
                    subchannel.requestConnection();
                }
                return managedSubchannel;
            }

            private void handleSubchannelState(ManagedSubchannel managedSubchannel,
                    ConnectivityStateInfo stateInfo, LoadBalancer.Helper helper) {
                if (subchannelsByAddress.get(managedSubchannel.addressKey) != managedSubchannel) {
                    return;
                }
                if (stateInfo.getState() == ConnectivityState.SHUTDOWN) {
                    return;
                }

                if (stateInfo.getState() == TRANSIENT_FAILURE) {
                    Status status = stateInfo.getStatus();
                    log.error("gRPC Sub Channel failed", status == null ? null : status.getCause());
                    helper.refreshNameResolution();
                } else if (stateInfo.getState() == IDLE) {
                    helper.refreshNameResolution();
                    if (requestConnections) {
                        managedSubchannel.subchannel.requestConnection();
                    }
                }
                log.debugf("subchannel changed state to %s for %s", stateInfo.getState(),
                        managedSubchannel.serviceInstance.getId());

                managedSubchannel.state = stateInfo.getState();
                managedSubchannel.status = stateInfo.getStatus();
                rebuildActiveServiceInstances();
                updateBalancingState(helper);
            }

            private void shutdownRemovedSubchannels(Set<AddressKey> desiredAddresses) {
                List<AddressKey> removedAddresses = new ArrayList<>();
                for (AddressKey addressKey : subchannelsByAddress.keySet()) {
                    if (!desiredAddresses.contains(addressKey)) {
                        removedAddresses.add(addressKey);
                    }
                }
                for (AddressKey addressKey : removedAddresses) {
                    ManagedSubchannel removed = subchannelsByAddress.remove(addressKey);
                    if (removed != null) {
                        removed.shutdown();
                    }
                }
            }

            private void rebuildActiveServiceInstances() {
                activeServiceInstances.clear();
                for (ManagedSubchannel managedSubchannel : subchannelsByAddress.values()) {
                    if (managedSubchannel.state == ConnectivityState.READY
                            && subchannelsByServiceInstance.containsKey(managedSubchannel.serviceInstance)) {
                        activeServiceInstances.add(managedSubchannel.serviceInstance);
                    }
                }
            }

            private void updateBalancingState(LoadBalancer.Helper helper) {
                if (subchannelsByServiceInstance.isEmpty()) {
                    helper.updateBalancingState(TRANSIENT_FAILURE,
                            new GrpcLoadBalancerProvider.ErrorPicker(
                                    Status.UNAVAILABLE.withDescription("No Stork service instances available")));
                    return;
                }

                ConnectivityState state = calculateState();
                if (state == TRANSIENT_FAILURE) {
                    helper.updateBalancingState(state, new GrpcLoadBalancerProvider.ErrorPicker(lastFailure()));
                } else {
                    helper.updateBalancingState(state, new StorkSubchannelPicker(
                            copySubchannelsByServiceInstance(),
                            serviceName,
                            new HashSet<>(activeServiceInstances)));
                }
            }

            private Map<ServiceInstance, Subchannel> copySubchannelsByServiceInstance() {
                Map<ServiceInstance, Subchannel> copy = new TreeMap<>(Comparator.comparingLong(ServiceInstance::getId));
                copy.putAll(subchannelsByServiceInstance);
                return copy;
            }

            private ConnectivityState calculateState() {
                if (!activeServiceInstances.isEmpty()) {
                    return ConnectivityState.READY;
                }
                boolean connectingOrIdle = false;
                boolean transientFailure = false;
                for (ManagedSubchannel managedSubchannel : subchannelsByAddress.values()) {
                    switch (managedSubchannel.state) {
                        case CONNECTING:
                        case IDLE:
                            connectingOrIdle = true;
                            break;
                        case TRANSIENT_FAILURE:
                            transientFailure = true;
                            break;
                        default:
                            break;
                    }
                }
                if (connectingOrIdle) {
                    return ConnectivityState.CONNECTING;
                }
                return transientFailure ? TRANSIENT_FAILURE : ConnectivityState.CONNECTING;
            }

            private Status lastFailure() {
                for (ManagedSubchannel managedSubchannel : subchannelsByAddress.values()) {
                    if (managedSubchannel.state == TRANSIENT_FAILURE) {
                        return managedSubchannel.status;
                    }
                }
                return Status.UNAVAILABLE;
            }
        };
    }

    static class StorkLoadBalancerConfig {
        final String serviceName;

        StorkLoadBalancerConfig(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    static final class AddressKey {
        private final List<SocketAddress> addresses;

        private AddressKey(List<SocketAddress> addresses) {
            this.addresses = addresses;
        }

        static AddressKey from(EquivalentAddressGroup addressGroup) {
            return new AddressKey(List.copyOf(addressGroup.getAddresses()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AddressKey)) {
                return false;
            }
            AddressKey that = (AddressKey) o;
            return addresses.equals(that.addresses);
        }

        @Override
        public int hashCode() {
            return addresses.hashCode();
        }
    }

    static class ManagedSubchannel {
        final AddressKey addressKey;
        final LoadBalancer.Subchannel subchannel;
        EquivalentAddressGroup addressGroup;
        ServiceInstance serviceInstance;
        ConnectivityState state = ConnectivityState.CONNECTING;
        Status status = Status.OK;

        ManagedSubchannel(AddressKey addressKey, LoadBalancer.Subchannel subchannel,
                EquivalentAddressGroup addressGroup, ServiceInstance serviceInstance) {
            this.addressKey = addressKey;
            this.subchannel = subchannel;
            this.addressGroup = addressGroup;
            this.serviceInstance = serviceInstance;
        }

        void update(EquivalentAddressGroup addressGroup, ServiceInstance serviceInstance) {
            this.addressGroup = addressGroup;
            this.serviceInstance = serviceInstance;
            subchannel.updateAddresses(List.of(addressGroup));
        }

        void shutdown() {
            subchannel.shutdown();
        }
    }

    static class StorkSubchannelPicker extends LoadBalancer.SubchannelPicker {
        private final Map<ServiceInstance, LoadBalancer.Subchannel> subChannels;
        private final String serviceName;
        private final Set<ServiceInstance> activeServiceInstances;

        StorkSubchannelPicker(Map<ServiceInstance, LoadBalancer.Subchannel> subChannels,
                String serviceName, Set<ServiceInstance> activeServiceInstances) {
            this.subChannels = subChannels;
            this.serviceName = serviceName;
            this.activeServiceInstances = activeServiceInstances;
        }

        @Override
        public LoadBalancer.PickResult pickSubchannel(LoadBalancer.PickSubchannelArgs args) {
            Boolean measureTime = STORK_MEASURE_TIME.get();
            measureTime = measureTime != null && measureTime;
            ServiceInstance serviceInstance = pickServerInstance(measureTime);
            LoadBalancer.Subchannel subchannel = subChannels.get(serviceInstance);

            if (serviceInstance.gatherStatistics() && STORK_SERVICE_INSTANCE.get() != null) {
                STORK_SERVICE_INSTANCE.get().set(serviceInstance);
                return LoadBalancer.PickResult.withSubchannel(subchannel);
            } else {
                return LoadBalancer.PickResult.withSubchannel(subchannel);
            }
        }

        private ServiceInstance pickServerInstance(boolean measureTime) {
            Service service = Stork.getInstance().getService(serviceName);

            Set<ServiceInstance> toChooseFrom = this.activeServiceInstances;
            if (activeServiceInstances.isEmpty()) {
                toChooseFrom = subChannels.keySet();
                log.debugf("no active service instances, using all subChannels: %s", toChooseFrom);
            }
            return service.selectInstanceAndRecordStart(toChooseFrom, measureTime);
        }
    }

    static class ErrorPicker extends LoadBalancer.SubchannelPicker {
        private final Status status;

        ErrorPicker(Status status) {
            this.status = status;
        }

        @Override
        public LoadBalancer.PickResult pickSubchannel(LoadBalancer.PickSubchannelArgs args) {
            return LoadBalancer.PickResult.withError(status);
        }
    }
}
