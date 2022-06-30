package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.openshift.config.OpenshiftConfigFluent;

public class ApplyRouteExpositionConfigurator extends Configurator<OpenshiftConfigFluent> {

    private final ExpositionConfig expositionConfig;

    public ApplyRouteExpositionConfigurator(ExpositionConfig expositionConfig) {
        this.expositionConfig = expositionConfig;
    }

    @Override
    public void visit(OpenshiftConfigFluent config) {
        if (expositionConfig.expose) {
            config.withExpose(true);
            if (expositionConfig.host.isPresent()) {
                config.withHost(expositionConfig.host.get());
            }
        }
    }
}
