package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.Classpath;

import io.quarkus.bootstrap.BootstrapConstants;

public abstract class DeploymentArtifactsValueSource
        implements ValueSource<List<String>, DeploymentArtifactsValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {

        @Classpath
        ConfigurableFileCollection getRuntimeArtifacts();
    }

    @Override
    public List<String> obtain() {
        List<DeploymentDependencySpec> deploymentArtifacts = new ArrayList<>();
        for (File runtimeArtifact : getParameters().getRuntimeArtifacts().getFiles()) {
            ExtensionDescriptorReader.readDescriptor(runtimeArtifact)
                    .map(properties -> deploymentArtifact(properties, runtimeArtifact))
                    .ifPresent(deploymentArtifacts::add);
        }
        return deploymentArtifacts.stream()
                .distinct()
                .sorted()
                .map(DeploymentDependencySpec::serialize)
                .toList();
    }

    private static DeploymentDependencySpec deploymentArtifact(Properties properties, File runtimeArtifact) {
        String deploymentArtifact = properties.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if (deploymentArtifact == null || deploymentArtifact.isBlank()) {
            throw new IllegalStateException("Quarkus extension descriptor in " + runtimeArtifact
                    + " does not declare " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        }
        return DeploymentDependencySpec.external(deploymentArtifact);
    }
}
