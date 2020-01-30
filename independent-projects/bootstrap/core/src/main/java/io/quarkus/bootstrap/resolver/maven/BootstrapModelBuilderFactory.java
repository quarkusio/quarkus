package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.validation.ModelValidator;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

class BootstrapModelBuilderFactory extends DefaultModelBuilderFactory {

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
                if(projectDir != null && !model.getProperties().isEmpty()) {
                    // this should be a project from the current workspace
                    model = ModelUtils.applySystemProperties(model);
                }
                return defaultInterpolator.interpolateModel(model, projectDir, request, problems);
            }
        };
    }
}