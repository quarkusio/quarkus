package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.prometheus.model.ServiceMonitorBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class AddServiceMonitorResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String scheme;
    private final String targetPort;
    private final String path;
    private final int interval;
    private final boolean honorLabels;

    public AddServiceMonitorResourceDecorator(String scheme, String targetPort, String path, int interval,
            boolean honorLabels) {
        this.scheme = scheme;
        this.targetPort = targetPort;
        this.path = path;
        this.interval = interval;
        this.honorLabels = honorLabels;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list, ANY);
        list.addToItems(new ServiceMonitorBuilder()
                .withNewMetadata()
                .withName(meta.getName())
                .withLabels(meta.getLabels())
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(meta.getLabels())
                .endSelector()
                .addNewEndpoint()
                .withScheme(scheme)
                .withNewTargetPort(Integer.parseInt(targetPort)) //This needs to be passed as int
                .withPath(path)
                .withInterval(interval + "s")
                .withHonorLabels(honorLabels)
                .endEndpoint()
                .endSpec());
    }
}
