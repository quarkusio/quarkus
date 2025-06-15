package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.*;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.openshift.decorator.ApplyDeploymentTriggerDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.DeploymentConfigSpecFluent;

public class ChangeContainerNameInDeploymentTriggerDecorator
        extends NamedResourceDecorator<DeploymentConfigSpecFluent<?>> {

    private final String containerName;

    public ChangeContainerNameInDeploymentTriggerDecorator(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public void andThenVisit(DeploymentConfigSpecFluent<?> deploymentConfigSpecFluent, ObjectMeta objectMeta) {
        if (deploymentConfigSpecFluent.hasTriggers()) {
            deploymentConfigSpecFluent.editFirstTrigger().editImageChangeParams().withContainerNames(containerName)
                    .endImageChangeParams().endTrigger().buildTriggers();
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyDeploymentTriggerDecorator.class, AddEnvVarDecorator.class, AddPortDecorator.class,
                AddMountDecorator.class, AddPvcVolumeDecorator.class, AddAwsElasticBlockStoreVolumeDecorator.class,
                AddAzureDiskVolumeDecorator.class, AddAwsElasticBlockStoreVolumeDecorator.class,
                ApplyImageDecorator.class, ApplyImagePullPolicyDecorator.class, ApplyWorkingDirDecorator.class,
                ApplyCommandDecorator.class, ApplyArgsDecorator.class, ApplyServiceAccountNamedDecorator.class,
                AddReadinessProbeDecorator.class, AddLivenessProbeDecorator.class,
                ApplyApplicationContainerDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class };
    }

}
