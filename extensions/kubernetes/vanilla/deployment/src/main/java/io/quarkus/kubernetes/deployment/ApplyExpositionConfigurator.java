package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.BaseConfigFluent;
import io.dekorate.kubernetes.config.Configurator;

public class ApplyExpositionConfigurator extends Configurator<BaseConfigFluent> {

    private final ExpositionConfig expositionConfig;

    public ApplyExpositionConfigurator(ExpositionConfig expositionConfig) {
        this.expositionConfig = expositionConfig;
    }

    @Override
    public void visit(BaseConfigFluent config) {
        if (expositionConfig.expose) {
            config.withExpose(true);
            if (expositionConfig.host.isPresent()) {
                config.withHost(expositionConfig.host.get());
            }
        }
    }
}
