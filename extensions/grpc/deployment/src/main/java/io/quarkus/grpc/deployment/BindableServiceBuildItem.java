package io.quarkus.grpc.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class BindableServiceBuildItem extends MultiBuildItem {

    final DotName serviceClass;
    final List<String> blockingMethods = new ArrayList<>();

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

    public boolean hasBlockingMethods() {
        return !blockingMethods.isEmpty();
    }

    public DotName getServiceClass() {
        return serviceClass;
    }

}
