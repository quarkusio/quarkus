package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.KubernetesConfigFluent;

public class ApplyKubernetesExpositionConfigurator extends Configurator<KubernetesConfigFluent> {

    private final ExpositionConfig expositionConfig;

    public ApplyKubernetesExpositionConfigurator(ExpositionConfig expositionConfig) {
        this.expositionConfig = expositionConfig;
    }

    @Override
    public void visit(KubernetesConfigFluent config) {
        if (expositionConfig.expose) {
            KubernetesConfigFluent.IngressNested ingressConfig = config.withNewIngress();
            ingressConfig.withExpose(true);
            expositionConfig.host.ifPresent(ingressConfig::withHost);

            ingressConfig.endIngress();
        }
    }
}
