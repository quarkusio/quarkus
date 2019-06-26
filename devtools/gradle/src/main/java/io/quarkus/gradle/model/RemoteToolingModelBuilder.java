package io.quarkus.gradle.model;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.gradle.tooling.DefaultRemoteAppModel;
import io.quarkus.bootstrap.resolver.gradle.tooling.RemoteAppModel;
import io.quarkus.gradle.QuarkusPluginExtension;

public class RemoteToolingModelBuilder implements ToolingModelBuilder {

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(RemoteAppModel.class.getName());
    }

    @Override
    public RemoteAppModel buildAll(String modelName, Project project) {
        QuarkusPluginExtension extension = project.getExtensions().findByType(QuarkusPluginExtension.class);
        AppArtifact artifact = extension.getAppArtifact();

        try {
            AppModel appModel = extension.resolveAppModel().resolveModel(artifact);
            return DefaultRemoteAppModel.from(appModel);
        } catch (Exception ex) {
            throw new GradleException("Unable to resolve AppModel for artifact (" + artifact + ")", ex);
        }
    }
}
