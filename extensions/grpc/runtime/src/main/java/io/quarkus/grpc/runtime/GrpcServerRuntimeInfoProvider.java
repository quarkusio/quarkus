package io.quarkus.grpc.runtime;

import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.ValueRegistry;

/**
 * Registers the {@link GrpcServer} with {@link ValueRegistry}.
 */
public class GrpcServerRuntimeInfoProvider implements RuntimeInfoProvider {

    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource source) {
        valueRegistry.registerInfo(GrpcServer.GRPC_SERVER, GrpcServer.INFO);
    }
}
