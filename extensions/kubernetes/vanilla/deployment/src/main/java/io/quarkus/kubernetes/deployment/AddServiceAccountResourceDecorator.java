package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CORE_API_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.SERVICE_ACCOUNT;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;

public class AddServiceAccountResourceDecorator extends BaseAddRBACDecorator<ServiceAccount, ServiceAccountBuilder> {
    private final String namespace;

    public AddServiceAccountResourceDecorator(String deploymentName, String name, String namespace,
            Map<String, String> labels) {
        super(name, SERVICE_ACCOUNT, CORE_API_VERSION, deploymentName, labels);
        this.namespace = namespace;
    }

    @Override
    protected ServiceAccountBuilder builderWithName(String name) {
        return new ServiceAccountBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(ServiceAccountBuilder builder, Void config) {
        updateMetadata(builder.editOrNewMetadata(), namespace)
                .endMetadata();
    }
}
