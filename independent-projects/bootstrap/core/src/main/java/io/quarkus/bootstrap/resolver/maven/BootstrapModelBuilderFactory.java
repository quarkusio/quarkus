package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.validation.ModelValidator;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

class BootstrapModelBuilderFactory extends DefaultModelBuilderFactory {

    private final WorkspaceModelResolver wsModelResolver;

    BootstrapModelBuilderFactory(WorkspaceModelResolver wsModelResolver) {
        this.wsModelResolver = wsModelResolver;
    }

    @Override
    protected ModelValidator newModelValidator() {
        return new ModelValidator() {
            @Override
            public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
            }

            @Override
            public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
            }};
    }

    @Override
    protected ModelInterpolator newModelInterpolator() {
        final ModelInterpolator defaultInterpolator = super.newModelInterpolator();
        return new ModelInterpolator() {
            @Override
            public Model interpolateModel(Model model, File projectDir, ModelBuildingRequest request,
                    ModelProblemCollector problems) {
                try {
                    if ((projectDir != null // if projectDir is not null, this project belongs to the current workspace
                            || ("pom".equals(model.getPackaging()) // if projectDir is null but the packaging is pom, it may still be coming from the current workspace
                                    && wsModelResolver != null
                                    && wsModelResolver.resolveRawModel(ModelUtils.getGroupId(model), model.getArtifactId(),
                                            ModelUtils.getVersion(model)) != null))
                            && !model.getProperties().isEmpty()) {
                        model = ModelUtils.applySystemProperties(model);
                    }
                } catch (UnresolvableModelException e) {
                    // it's unlikely this is going to be thrown and if it is it can be ignored
                }
                return defaultInterpolator.interpolateModel(model, projectDir, request, problems);
            }
        };
    }
}