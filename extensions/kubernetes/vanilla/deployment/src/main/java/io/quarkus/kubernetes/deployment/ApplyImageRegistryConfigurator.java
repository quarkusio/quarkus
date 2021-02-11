
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.ImageConfigurationFluent;

public class ApplyImageRegistryConfigurator extends Configurator<ImageConfigurationFluent<?>> {

    private final String registry;

    public ApplyImageRegistryConfigurator(String registry) {
        this.registry = registry;
    }

    @Override
    public void visit(ImageConfigurationFluent<?> config) {
        config.withRegistry(registry);
    }
}
