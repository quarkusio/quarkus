package io.quarkus.bootstrap.resolver.maven;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.validation.ModelValidator;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenModelBuilder implements ModelBuilder {

    private final ModelBuilder builder;
    private final WorkspaceModelResolver modelResolver;

    public MavenModelBuilder(WorkspaceModelResolver wsModelResolver) {
        builder = new DefaultModelBuilderFactory().newInstance()
                .setModelValidator(new ModelValidator() {
                    @Override
                    public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
                    }

                    @Override
                    public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
                    }
                });

        modelResolver = wsModelResolver;
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        if(modelResolver != null) {
            request.setWorkspaceModelResolver(modelResolver);
        }
        return builder.build(request);
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request, ModelBuildingResult result) throws ModelBuildingException {
        return builder.build(request, result);
    }

    @Override
    public Result<? extends Model> buildRawModel(File pomFile, int validationLevel, boolean locationTracking) {
        return builder.buildRawModel(pomFile, validationLevel, locationTracking);
    }

}
