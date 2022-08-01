package io.quarkus.bootstrap.resolver.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.validation.ModelValidator;

public class BootstrapModelBuilderFactory extends DefaultModelBuilderFactory {

    private static ModelBuilder delegate;

    public static ModelBuilder getDefaultModelBuilder() {
        return delegate == null ? delegate = new BootstrapModelBuilderFactory().newInstance() : delegate;
    }

    @Override
    protected ModelValidator newModelValidator() {
        return new ModelValidator() {
            @Override
            public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
            }

            @Override
            public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
            }
        };
    }
}