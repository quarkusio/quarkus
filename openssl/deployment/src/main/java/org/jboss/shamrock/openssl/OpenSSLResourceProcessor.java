package org.jboss.shamrock.openssl;

import javax.inject.Inject;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;

public class OpenSSLResourceProcessor {

    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

    @BuildStep
    public void build() throws Exception {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.wildfly.openssl.OpenSSLEngine"));
    }
}
