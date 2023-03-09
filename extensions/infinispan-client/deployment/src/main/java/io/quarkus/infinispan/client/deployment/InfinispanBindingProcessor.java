
package io.quarkus.infinispan.client.deployment;

import static io.quarkus.infinispan.client.runtime.InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;

public class InfinispanBindingProcessor {

    private static final String INFINISPAN = "infinispan";

    @BuildStep
    public void process(List<InfinispanClientBuildItem> clients,
            BuildProducer<ServiceBindingQualifierBuildItem> bindings) {
        clients.forEach(client -> {
            if (InfinispanClientUtil.isDefault(client.getName())) {
                bindings.produce(
                        new ServiceBindingQualifierBuildItem(INFINISPAN, INFINISPAN, DEFAULT_INFINISPAN_CLIENT_NAME));
            } else {
                bindings.produce(new ServiceBindingQualifierBuildItem(INFINISPAN, client.getName()));
            }
        });
    }
}
