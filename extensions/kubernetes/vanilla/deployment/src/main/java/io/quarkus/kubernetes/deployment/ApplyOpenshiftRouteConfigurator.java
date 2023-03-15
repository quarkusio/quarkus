package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.openshift.config.OpenshiftConfigFluent;
import io.dekorate.openshift.config.TLSConfig;

public class ApplyOpenshiftRouteConfigurator extends Configurator<OpenshiftConfigFluent> {

    private final RouteConfig routeConfig;

    public ApplyOpenshiftRouteConfigurator(RouteConfig routeConfig) {
        this.routeConfig = routeConfig;
    }

    @Override
    public void visit(OpenshiftConfigFluent config) {
        if (routeConfig.expose) {
            var routeBuilder = config.editOrNewRoute();
            routeBuilder.withExpose(true);
            if (routeConfig.host.isPresent()) {
                routeBuilder.withHost(routeConfig.host.get());
            }

            routeBuilder.withTargetPort(routeConfig.targetPort);
            if (routeConfig.tls != null) {
                TLSConfig tls = new TLSConfig();
                routeConfig.tls.caCertificate.ifPresent(tls::setCaCertificate);
                routeConfig.tls.certificate.ifPresent(tls::setCertificate);
                routeConfig.tls.destinationCACertificate.ifPresent(tls::setDestinationCACertificate);
                routeConfig.tls.key.ifPresent(tls::setKey);
                routeConfig.tls.termination.ifPresent(tls::setTermination);
                routeConfig.tls.insecureEdgeTerminationPolicy.ifPresent(tls::setInsecureEdgeTerminationPolicy);
                routeBuilder.withTls(tls);
            }
            routeBuilder.endRoute();
        }
    }
}
