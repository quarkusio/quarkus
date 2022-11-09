package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.openshift.config.OpenshiftConfigFluent;

public class ApplyOpenshiftRouteConfigurator extends Configurator<OpenshiftConfigFluent> {

    private final RouteConfig routeConfig;
    private final boolean defaultExpose;

    public ApplyOpenshiftRouteConfigurator(RouteConfig routeConfig, boolean defaultExpose) {
        this.routeConfig = routeConfig;
        this.defaultExpose = defaultExpose;
    }

    @Override
    public void visit(OpenshiftConfigFluent config) {
        if (routeConfig.expose || defaultExpose) {
            var routeBuilder = config.editOrNewRoute();
            routeBuilder.withExpose(true);
            if (routeConfig.host.isPresent()) {
                routeBuilder.withHost(routeConfig.host.get());
            }

            if (routeConfig.targetPort.isPresent()) {
                routeBuilder.withTargetPort(routeConfig.targetPort.get());
            }

            routeBuilder.endRoute();
        }
    }
}
