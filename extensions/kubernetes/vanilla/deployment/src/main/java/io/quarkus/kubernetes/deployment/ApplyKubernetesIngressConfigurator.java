package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.KubernetesConfigFluent;

public class ApplyKubernetesIngressConfigurator extends Configurator<KubernetesConfigFluent> {

    private final IngressConfig ingressConfig;

    public ApplyKubernetesIngressConfigurator(IngressConfig ingressConfig) {
        this.ingressConfig = ingressConfig;
    }

    @Override
    public void visit(KubernetesConfigFluent config) {
        if (ingressConfig.expose()) {
            KubernetesConfigFluent.IngressNested ingressConfigBuilder = config.withNewIngress();
            ingressConfigBuilder.withExpose(true);
            ingressConfig.host().ifPresent(ingressConfigBuilder::withHost);
            ingressConfigBuilder.withTargetPort(ingressConfig.targetPort());
            ingressConfig.ingressClassName().ifPresent(ingressConfigBuilder::withIngressClassName);

            ingressConfigBuilder.endIngress();
        }
    }
}
