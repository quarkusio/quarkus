
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.s2i.config.S2iBuildConfigFluent;

public class DisableS2iConfigurator extends Configurator<S2iBuildConfigFluent<?>> {

    @Override
    public void visit(S2iBuildConfigFluent<?> s2i) {
        s2i.withEnabled(false);
    }
}
