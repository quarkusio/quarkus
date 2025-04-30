package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.openshift.config.OpenshiftConfigFluent;

public class AddPortToOpenshiftConfig extends Configurator<OpenshiftConfigFluent<?>> {

    private final Port port;

    public AddPortToOpenshiftConfig(Port port) {
        this.port = port;
    }

    @Override
    public void visit(OpenshiftConfigFluent<?> config) {
        if (!hasPort(config)) {
            config.addToPorts(port);
        }
    }

    /**
     * Check if the {@link OpenShiftConfig} already has port.
     *
     * @param config The port.
     * @return True if port with same container port exists.
     */
    private boolean hasPort(OpenshiftConfigFluent<?> config) {
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
        AddPortToOpenshiftConfig addPort = (AddPortToOpenshiftConfig) o;
        return Objects.equals(port, addPort.port);
    }

    @Override
    public int hashCode() {

        return Objects.hash(port);
    }
}
