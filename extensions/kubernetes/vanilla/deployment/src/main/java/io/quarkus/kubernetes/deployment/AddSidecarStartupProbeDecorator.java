package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Probe;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.AddStartupProbeDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

/**
 * Adds a startup probe to a sidecar container. The Dekorate container configuration used to add sidecars only
 * carries liveness and readiness probes, so the startup probe is applied afterwards to the container by name.
 */
public class AddSidecarStartupProbeDecorator extends AddStartupProbeDecorator {

    public AddSidecarStartupProbeDecorator(String deploymentName, String containerName, Probe probe) {
        super(deploymentName, containerName, probe);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class };
    }
}
