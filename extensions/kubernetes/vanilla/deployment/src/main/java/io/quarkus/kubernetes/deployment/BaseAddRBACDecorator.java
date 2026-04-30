package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;

import java.util.List;
import java.util.Map;

import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;
import io.fabric8.kubernetes.api.model.rbac.Subject;

abstract class BaseAddRBACDecorator<T extends HasMetadata, B extends VisitableBuilder<T, B>>
        extends BaseAddResourceDecorator<T, B, Void> {

    private final String deploymentName;
    private Map<String, String> labels;

    public BaseAddRBACDecorator(String name, String kind, String deploymentName, Map<String, String> labels) {
        super(name, kind, RBAC_API_VERSION);
        this.labels = labels;
        this.deploymentName = deploymentName;
    }

    protected BaseAddRBACDecorator(String name, String kind, String apiVersion, String deploymentName,
            Map<String, String> labels) {
        super(name, kind, apiVersion);
        this.deploymentName = deploymentName;
        this.labels = labels;
    }

    protected String deploymentName() {
        return deploymentName;
    }

    /**
     * Extract potential labels to add to the RBAC resource if needed
     */
    protected void prepare(List<HasMetadata> items, KubernetesListBuilder list) {
        labels = mergeLabelsFromDeploymentWith(items, labels, deploymentName);
    }

    protected <O extends ObjectMetaFluent<?>> O updateMetadata(O builder, String namespace) {
        return updateMetadata(builder, namespace, labels);
    }

    protected Subject createRBACSubject(io.quarkus.kubernetes.spi.Subject subject) {
        return new Subject(subject.getApiGroup(), subject.getKind(),
                Strings.defaultIfEmpty(subject.getName(), deploymentName()), subject.getNamespace());
    }
}
