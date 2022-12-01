package io.quarkus.arc.deployment;

import io.quarkus.arc.ClientProxy;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ProxyUnwrapperBuildItem;

public class ProxyUnwrapProcessor {

    @BuildStep
    ProxyUnwrapperBuildItem wrapper() {
        return new ProxyUnwrapperBuildItem(ClientProxy::unwrap);
    }
}
