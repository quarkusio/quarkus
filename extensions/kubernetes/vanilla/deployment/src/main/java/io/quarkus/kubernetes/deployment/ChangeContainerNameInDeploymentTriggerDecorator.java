package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddAwsElasticBlockStoreVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureDiskVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddPortDecorator;
import io.dekorate.kubernetes.decorator.AddPvcVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplyApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyServiceAccountNamedDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.openshift.decorator.ApplyDeploymentTriggerDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.DeploymentConfigSpecFluent;

public class ChangeContainerNameInDeploymentTriggerDecorator extends NamedResourceDecorator<DeploymentConfigSpecFluent<?>> {

    private final String containerName;

    public ChangeContainerNameInDeploymentTriggerDecorator(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public void andThenVisit(DeploymentConfigSpecFluent<?> deploymentConfigSpecFluent, ObjectMeta objectMeta) {
        if (deploymentConfigSpecFluent.hasTriggers()) {
            deploymentConfigSpecFluent
                    .editFirstTrigger()
                    .editImageChangeParams()
                    .withContainerNames(containerName)
                    .endImageChangeParams()
                    .endTrigger()
                    .buildTriggers();
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyDeploymentTriggerDecorator.class, AddEnvVarDecorator.class, AddPortDecorator.class,
                AddMountDecorator.class, AddPvcVolumeDecorator.class, AddAwsElasticBlockStoreVolumeDecorator.class,
                AddAzureDiskVolumeDecorator.class, AddAwsElasticBlockStoreVolumeDecorator.class, ApplyImageDecorator.class,
                ApplyImagePullPolicyDecorator.class, ApplyWorkingDirDecorator.class, ApplyCommandDecorator.class,
                ApplyArgsDecorator.class, ApplyServiceAccountNamedDecorator.class, AddReadinessProbeDecorator.class,
                AddLivenessProbeDecorator.class, ApplyApplicationContainerDecorator.class, AddSidecarDecorator.class,
                AddInitContainerDecorator.class };
    }

}
