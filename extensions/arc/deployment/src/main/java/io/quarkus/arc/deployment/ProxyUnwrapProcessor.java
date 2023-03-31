package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.ClientProxyUnwrapper;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ProxyUnwrapperBuildItem;

public class ProxyUnwrapProcessor {

    @BuildStep
    ProxyUnwrapperBuildItem wrapper() {
        // ClientProxy::unwrap cannot be used because we need to serialize the unwrapper through a recorder method
        return new ProxyUnwrapperBuildItem(new ClientProxyUnwrapper());
    }
}
