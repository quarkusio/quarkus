
package io.quarkus.mongodb.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceQualifierBuildItem;

public class MongoBindingProcessor {

    private static final String MONGO = "mongodb";

    @BuildStep
    public void process(MongoClientBuildTimeConfig config, List<MongoClientBuildItem> clients,
            BuildProducer<ServiceQualifierBuildItem> bindings) {
        clients.forEach(client -> {
            bindings.produce(new ServiceQualifierBuildItem(MONGO, client.getName()));
        });
    }
}
