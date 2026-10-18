package io.quarkus.grpc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalGlobalInterceptorBuildItem extends MultiBuildItem {
    private final String interceptorClass;

    public AdditionalGlobalInterceptorBuildItem(String interceptorClass) {
        this.interceptorClass = interceptorClass;
    }

    public String interceptorClass() {
        return interceptorClass;
    }
}
