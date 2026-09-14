package io.quarkus.grpc.runtime.stork;

import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolverRegistry;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GrpcStorkRecorder {
    public void init(ShutdownContext shutdown, boolean proactiveConnections) {
        NameResolverRegistry.getDefaultRegistry().register(new GrpcStorkServiceDiscovery());
        LoadBalancerRegistry.getDefaultRegistry().register(new GrpcLoadBalancerProvider(proactiveConnections));
        shutdown.addShutdownTask(StorkServiceObservationSupport::clear);
    }
}
