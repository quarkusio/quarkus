package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.RoutePort;
import io.fabric8.openshift.api.model.RouteSpecFluent;

public class ApplyRouteTargetPortDecorator extends NamedResourceDecorator<RouteSpecFluent<?>> {

    private IntOrString targetPort;

    public ApplyRouteTargetPortDecorator(Integer port) {
        super(ANY);
        targetPort = new IntOrString(port);
    }

    public ApplyRouteTargetPortDecorator(String port) {
        super(ANY);
        targetPort = new IntOrString(port);
    }

    @Override
    public void andThenVisit(RouteSpecFluent routeSpecFluent, ObjectMeta objectMeta) {
        routeSpecFluent.withPort(new RoutePort(targetPort));
    }
}