package io.quarkus.grpc.runtime;

import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.ValueRegistry;

/**
 * Registers the {@link GrpcServer} with {@link ValueRegistry}.
 * <p>
 * In normal mode, the {@link GrpcServer} is also registered with a CDI Bean to support injection.
 */
public class GrpcServerRuntimeInfoProvider implements RuntimeInfoProvider {

    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource source) {
        valueRegistry.registerInfo(GrpcServer.GRPC_SERVER, GrpcServer.INFO);

        Boolean separateServer = source.get(GrpcServer.GRPC_SEPARATE_SERVER);
        if (separateServer != null) {
            valueRegistry.register(GrpcServer.GRPC_SEPARATE_SERVER, separateServer);
        }
    }
}
