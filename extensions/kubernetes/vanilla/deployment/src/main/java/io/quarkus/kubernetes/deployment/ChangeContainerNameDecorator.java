package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddPortDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyLimitsCpuDecorator;
import io.dekorate.kubernetes.decorator.ApplyLimitsMemoryDecorator;
import io.dekorate.kubernetes.decorator.ApplyPortNameDecorator;
import io.dekorate.kubernetes.decorator.ApplyRequestsCpuDecorator;
import io.dekorate.kubernetes.decorator.ApplyRequestsMemoryDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;

public class ChangeContainerNameDecorator extends ApplicationContainerDecorator<ContainerFluent<?>> {

    private final String name;

    public ChangeContainerNameDecorator(String name) {
        this.name = name;
    }

    @Override
    public void andThenVisit(ContainerFluent<?> containerFluent) {
        containerFluent.withName(name);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyRequestsMemoryDecorator.class, AddEnvVarDecorator.class, AddMountDecorator.class,
                AddPortDecorator.class, ApplyArgsDecorator.class, ApplyCommandDecorator.class,
                ApplyImagePullPolicyDecorator.class, ApplyLimitsCpuDecorator.class, ApplyLimitsMemoryDecorator.class,
                ApplyPortNameDecorator.class, ApplyRequestsCpuDecorator.class, ApplyWorkingDirDecorator.class,
                ResourceProvidingDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class,
                AddLivenessProbeDecorator.class, AddReadinessProbeDecorator.class, ApplyImageDecorator.class };
    }

}
