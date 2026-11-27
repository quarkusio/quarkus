package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.ROUTE;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.openshift.decorator.AddPortToRouteDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.RouteSpecFluent;

/**
 * Sets {@code spec.path} of an OpenShift Route, which the Dekorate route configuration cannot express.
 */
public class ApplyPathToRouteDecorator extends NamedResourceDecorator<RouteSpecFluent<?>> {

    private final String path;

    public ApplyPathToRouteDecorator(String name, String path) {
        super(ROUTE, name);
        this.path = path;
    }

    @Override
    public void andThenVisit(RouteSpecFluent<?> spec, ObjectMeta resourceMeta) {
        spec.withPath(path);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddPortToRouteDecorator.class };
    }
}
