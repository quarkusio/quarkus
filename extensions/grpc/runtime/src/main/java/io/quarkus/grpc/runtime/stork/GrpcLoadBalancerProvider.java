package io.quarkus.grpc.runtime.stork;

import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_MEASURE_TIME;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_SERVICE_INSTANCE;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
     * @param requestConnections
     *        if true, the load balancer will proactively request connections from available channels. This leads to
     *        better load balancing at the cost of keeping active connections.
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
        return NameResolver.ConfigOrError.fromConfig(new StorkLoadBalancerConfig(serviceName));
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
        return new LoadBalancer() {

            String serviceName;

            @Override
            public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
                List<EquivalentAddressGroup> addresses = resolvedAddresses.getAddresses();

                Object loadBalancerConfig = resolvedAddresses.getLoadBalancingPolicyConfig();
                if (!(loadBalancerConfig instanceof StorkLoadBalancerConfig)) {
                    throw new IllegalStateException(
                            "invalid configuration for a Stork Load Balancer : " + loadBalancerConfig);
                }

                StorkLoadBalancerConfig config = (StorkLoadBalancerConfig) loadBalancerConfig;

                Map<ServiceInstance, Subchannel> subChannels = new TreeMap<>(
                        Comparator.comparingLong(ServiceInstance::getId));
                Set<ServiceInstance> activeSubchannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
                AtomicReference<ConnectivityState> state = new AtomicReference<>(ConnectivityState.CONNECTING);

                serviceName = config.serviceName;

                final StorkSubchannelPicker picker = new StorkSubchannelPicker(subChannels, serviceName,
                        activeSubchannels);

                for (EquivalentAddressGroup addressGroup : addresses) {
                    ServiceInstance serviceInstance = addressGroup.getAttributes()
                            .get(GrpcStorkServiceDiscovery.SERVICE_INSTANCE);
                    CreateSubchannelArgs subChannelArgs = CreateSubchannelArgs.newBuilder().setAddresses(addressGroup)
                            .setAttributes(addressGroup.getAttributes()).build();

                    Subchannel subchannel = helper.createSubchannel(subChannelArgs);
                    subchannel.start(new SubchannelStateListener() {
                        @Override
                        public void onSubchannelState(ConnectivityStateInfo stateInfo) {
                            if (stateInfo.getState() == TRANSIENT_FAILURE || stateInfo.getState() == IDLE) {
                                Status status = stateInfo.getStatus();
                                log.error("gRPC Sub Channel failed", status == null ? null : status.getCause());
                                helper.refreshNameResolution();
                            }
                            log.debugf("subchannel changed state to %s for %s", stateInfo.getState(),
                                    serviceInstance.getId());
                            switch (stateInfo.getState()) {
                                case READY:
                                    activeSubchannels.add(serviceInstance);
                                    if (state.getAndSet(ConnectivityState.READY) != ConnectivityState.READY) {
                                        helper.updateBalancingState(state.get(), picker);
                                    }
                                    break;
                                case CONNECTING:
                                case TRANSIENT_FAILURE:
                                case IDLE:
                                case SHUTDOWN:
                                    activeSubchannels.remove(serviceInstance);
                                    if (activeSubchannels.isEmpty()
                                            && state.compareAndSet(ConnectivityState.READY, stateInfo.getState())) {
                                        helper.updateBalancingState(state.get(), picker);
                                    }
                                    break;
                            }
                        }
                    });
                    if (requestConnections) {
                        subchannel.requestConnection();
                    }
                    subChannels.put(serviceInstance, subchannel);
                }

                helper.updateBalancingState(state.get(), picker);
            }

            @Override
            public void handleNameResolutionError(Status error) {
                log.errorf("Name resolution failed for service '%s'", serviceName);
            }

            @Override
            public void shutdown() {
                log.debugf("Shutting down load balancer for service '%s'", serviceName);
            }
        };
    }

    static class StorkLoadBalancerConfig {
        final String serviceName;

        StorkLoadBalancerConfig(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    static class StorkSubchannelPicker extends LoadBalancer.SubchannelPicker {
        private final Map<ServiceInstance, LoadBalancer.Subchannel> subChannels;
        private final String serviceName;
        private final Set<ServiceInstance> activeServiceInstances;

        StorkSubchannelPicker(Map<ServiceInstance, LoadBalancer.Subchannel> subChannels, String serviceName,
                Set<ServiceInstance> activeServiceInstances) {
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
}
