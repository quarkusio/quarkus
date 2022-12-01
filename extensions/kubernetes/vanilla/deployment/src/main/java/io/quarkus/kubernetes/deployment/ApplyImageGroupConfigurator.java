
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.ImageConfigurationFluent;

public class ApplyImageGroupConfigurator extends Configurator<ImageConfigurationFluent<?>> {

    private final String group;

    public ApplyImageGroupConfigurator(String group) {
        this.group = group;
    }

    @Override
    public void visit(ImageConfigurationFluent<?> config) {
        config.withGroup(group);
    }

}
