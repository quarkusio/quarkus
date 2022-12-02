package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.util.Optional;
import java.util.function.Predicate;

import org.jboss.logging.Logger;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecFluent;

public class AddNodePortDecorator extends NamedResourceDecorator<ServiceSpecFluent> {

    private static final Logger log = Logger.getLogger(AddNodePortDecorator.class);

    private final int nodePort;
    private final Optional<String> matchingPortName;

    public AddNodePortDecorator(String name, int nodePort) {
        this(name, nodePort, Optional.empty());
    }

    public AddNodePortDecorator(String name, int nodePort, Optional<String> matchingPortName) {
        super(name);
        if (nodePort < MIN_NODE_PORT_VALUE || nodePort > MAX_NODE_PORT_VALUE) {
            log.info("Using a port outside of the " + MIN_NODE_PORT_VALUE + "-" + MAX_NODE_PORT_VALUE
                    + " range might not work, see https://kubernetes.io/docs/concepts/services-networking/service/#nodeport");
        }
        this.nodePort = nodePort;
        this.matchingPortName = matchingPortName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void andThenVisit(ServiceSpecFluent service, ObjectMeta resourceMeta) {
        ServiceSpecFluent.PortsNested<?> editPort;
        if (matchingPortName.isPresent()) {
            editPort = service.editMatchingPort(new Predicate<ServicePortBuilder>() {
                @Override
                public boolean test(ServicePortBuilder servicePortBuilder) {
                    return (servicePortBuilder.hasName())
                            && (servicePortBuilder.getName().equals(matchingPortName.get()));
                }
            });
        } else {
            editPort = service.editFirstPort();
        }
        editPort.withNodePort(nodePort);
        editPort.endPort();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyServiceTypeDecorator.class };
    }
}
