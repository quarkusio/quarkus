package io.quarkus.grpc.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class BindableServiceBuildItem extends MultiBuildItem {

    final DotName serviceClass;
    final List<String> blockingMethods = new ArrayList<>();
    final List<String> virtualMethods = new ArrayList<>();

    public BindableServiceBuildItem(DotName serviceClass) {
        this.serviceClass = serviceClass;
    }

    /**
     * A method from {@code serviceClass} is annotated with {@link io.smallrye.common.annotation.Blocking}.
     * Stores the method name so the runtime interceptor can recognize it.
     * Note: gRPC method have unique names - overloading is not permitted.
     *
     * @param method the method name
     */
    public void registerBlockingMethod(String method) {
        blockingMethods.add(method);
    }

    /**
     * A method from {@code serviceClass} is annotated with {@link io.smallrye.common.annotation.RunOnVirtualThread}.
     * Stores the method name so the runtime interceptor can recognize it.
     * Note: gRPC method have unique names - overloading is not permitted.
     *
     * @param method the method name
     */
    public void registerVirtualMethod(String method) {
        virtualMethods.add(method);
    }

    public boolean hasBlockingMethods() {
        return !blockingMethods.isEmpty();
    }

    public boolean hasVirtualMethods() {
        return !virtualMethods.isEmpty();
    }

    public DotName getServiceClass() {
        return serviceClass;
    }

}
