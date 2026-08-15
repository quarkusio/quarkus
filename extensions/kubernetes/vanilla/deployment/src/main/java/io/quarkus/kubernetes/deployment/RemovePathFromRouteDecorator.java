package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.ROUTE;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.openshift.decorator.AddPortToRouteDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.RouteSpecFluent;

/**
 * Clears {@code spec.path} from an OpenShift Route.
 * <p>
 * OpenShift rejects any path (including {@code /}) when TLS termination is
 * {@code passthrough}, because the router cannot inspect the HTTP path.
 */
public class RemovePathFromRouteDecorator extends NamedResourceDecorator<RouteSpecFluent<?>> {

    public RemovePathFromRouteDecorator(String name) {
        super(ROUTE, name);
    }

    @Override
    public void andThenVisit(RouteSpecFluent<?> spec, ObjectMeta resourceMeta) {
        spec.withPath(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddPortToRouteDecorator.class };
    }
}
