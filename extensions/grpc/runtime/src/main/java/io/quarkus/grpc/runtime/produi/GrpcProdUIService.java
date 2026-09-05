package io.quarkus.grpc.runtime.produi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.GrpcServerRecorder.GrpcServiceDefinition;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the registered gRPC services. It exposes each
 * service's name, serving status, implementation class and the methods it
 * defines (name + streaming type), derived from the always-present
 * {@link GrpcServerRecorder#getServices()} registry. It deliberately omits the
 * Dev UI's invoke / test client: there is no way to call a service method or
 * send a message from this view.
 */
@ApplicationScoped
public class GrpcProdUIService {

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the registered gRPC services, their serving status and their methods")
    public List<ServiceInfo> getServices() {
        List<ServiceInfo> result = new ArrayList<>();
        for (GrpcServiceDefinition service : GrpcServerRecorder.getServices()) {
            ServerServiceDefinition definition = service.definition;
            String name = definition.getServiceDescriptor().getName();

            List<MethodInfo> methods = new ArrayList<>();
            for (ServerMethodDefinition<?, ?> method : definition.getMethods()) {
                methods.add(new MethodInfo(method.getMethodDescriptor().getBareMethodName(),
                        method.getMethodDescriptor().getType().name()));
            }
            methods.sort(Comparator.comparing(MethodInfo::name));

            result.add(new ServiceInfo(name, statusFor(name), service.getImplementationClassName(), methods));
        }
        result.sort(Comparator.comparing(ServiceInfo::name));
        return result;
    }

    private String statusFor(String serviceName) {
        InstanceHandle<GrpcHealthStorage> handle = Arc.container().instance(GrpcHealthStorage.class);
        if (!handle.isAvailable()) {
            return ServingStatus.UNKNOWN.name();
        }
        return handle.get().getStatuses().getOrDefault(serviceName, ServingStatus.UNKNOWN).name();
    }

    public record ServiceInfo(String name, String status, String serviceClass, List<MethodInfo> methods) {
    }

    public record MethodInfo(String name, String type) {
    }
}
