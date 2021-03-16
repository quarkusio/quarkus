package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import org.jboss.logging.Logger;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.ServiceSpecFluent;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;

public class AddNodePortDecorator extends NamedResourceDecorator<ServiceSpecFluent> {

    private static final Logger log = Logger.getLogger(AddNodePortDecorator.class);

    private final int nodePort;

    public AddNodePortDecorator(String name, int nodePort) {
        super(name);
        if (nodePort < MIN_NODE_PORT_VALUE || nodePort > MAX_NODE_PORT_VALUE) {
            log.info("Using a port outside of the " + MIN_NODE_PORT_VALUE + "-" + MAX_NODE_PORT_VALUE
                    + " range might not work, see https://kubernetes.io/docs/concepts/services-networking/service/#nodeport");
        }
        this.nodePort = nodePort;
    }

    @Override
    public void andThenVisit(ServiceSpecFluent service, ObjectMeta resourceMeta) {
        ServiceSpecFluent.PortsNested<?> editFirstPort = service.editFirstPort();
        editFirstPort.withNodePort(nodePort);
        editFirstPort.endPort();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyServiceTypeDecorator.class };
    }
}
