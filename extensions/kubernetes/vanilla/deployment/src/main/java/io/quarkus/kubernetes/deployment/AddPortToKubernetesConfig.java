package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.KubernetesConfigFluent;
import io.dekorate.kubernetes.config.Port;

public class AddPortToKubernetesConfig extends Configurator<KubernetesConfigFluent<?>> {

    private final Port port;

    public AddPortToKubernetesConfig(Port port) {
        this.port = port;
    }

    @Override
    public void visit(KubernetesConfigFluent<?> config) {
        if (!hasPort(config)) {
            config.addToPorts(port);
        }
    }

    /**
     * Check if the {@link KubernetesConfig} already has port.
     *
     * @param config The port.
     * @return True if port with same container port exists.
     */
    private boolean hasPort(KubernetesConfigFluent<?> config) {
        for (Port p : config.buildPorts()) {
            if (Objects.equals(p.getContainerPort(), port.getContainerPort())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AddPortToKubernetesConfig addPort = (AddPortToKubernetesConfig) o;
        return Objects.equals(port, addPort.port);
    }

    @Override
    public int hashCode() {

        return Objects.hash(port);
    }
}
