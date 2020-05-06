package io.quarkus.grpc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class BindableServiceBuildItem extends MultiBuildItem {

    final DotName serviceClass;

    public BindableServiceBuildItem(DotName serviceClass) {
        this.serviceClass = serviceClass;
    }

}
