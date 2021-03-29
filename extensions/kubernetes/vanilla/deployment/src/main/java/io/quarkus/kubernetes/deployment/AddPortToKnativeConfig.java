package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.knative.config.KnativeConfigFluent;
import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.Port;

public class AddPortToKnativeConfig extends Configurator<KnativeConfigFluent<?>> {

    private final Port port;

    public AddPortToKnativeConfig(Port port) {
        this.port = port;
    }

    @Override
    public void visit(KnativeConfigFluent<?> config) {
        if (!hasPort(config)) {
            config.addToPorts(port);
        }
    }

    /**
     * Check if the {@link KnativeConfig} already has port.
     * 
     * @param config The port.
     * @return True if port with same container port exists.
     */
    private boolean hasPort(KnativeConfigFluent<?> config) {
        for (Port p : config.getPorts()) {
            if (p.getContainerPort() == port.getContainerPort()) {
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
        AddPortToKnativeConfig addPort = (AddPortToKnativeConfig) o;
        return Objects.equals(port, addPort.port);
    }

    @Override
    public int hashCode() {

        return Objects.hash(port);
    }
}
