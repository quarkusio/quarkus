package io.quarkus.grpc.runtime.stork;

import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolverRegistry;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GrpcStorkRecorder {
    public void init() {
        NameResolverRegistry.getDefaultRegistry().register(new GrpcStorkServiceDiscovery());
        LoadBalancerRegistry.getDefaultRegistry().register(new GrpcLoadBalancerProvider());
    }
}
