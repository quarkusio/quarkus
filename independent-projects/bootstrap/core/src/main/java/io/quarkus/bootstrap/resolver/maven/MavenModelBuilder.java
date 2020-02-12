package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenModelBuilder implements ModelBuilder {

    private final ModelBuilder builder;
    private final WorkspaceModelResolver workspaceResolver;

    public MavenModelBuilder(WorkspaceModelResolver wsModelResolver) {
        builder = new BootstrapModelBuilderFactory().newInstance();
        workspaceResolver = wsModelResolver;
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        if (workspaceResolver != null) {
            request.setWorkspaceModelResolver(workspaceResolver);
            final Model requestModel = getModel(request);
            try {
                if (requestModel != null && workspaceResolver.resolveRawModel(ModelUtils.getGroupId(requestModel),
                        requestModel.getArtifactId(), ModelUtils.getVersion(requestModel)) != null) {
                    completeWorkspaceProjectBuildRequest(request);
                }
            } catch (UnresolvableModelException e) {
                // ignore
            }
        }
        return builder.build(request);
    }

    private Model getModel(ModelBuildingRequest request) {
        Model requestModel = request.getRawModel();
        if (requestModel == null) {
            if (request.getModelSource() != null) {
                try {
                    requestModel = ModelUtils.readModel(request.getModelSource().getInputStream());
                    request.setRawModel(requestModel);
                    if(request.getPomFile() != null) {
                        requestModel.setPomFile(request.getPomFile());
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return requestModel;
    }

    private void completeWorkspaceProjectBuildRequest(ModelBuildingRequest request) {
        final Model requestModel = getModel(request);
        if (!requestModel.getProfiles().isEmpty()) {
            request.getProfiles().addAll(requestModel.getProfiles());
        }
        final BootstrapMavenOptions mvnArgs = MavenRepoInitializer.getBootstrapMavenOptions();
        request.getActiveProfileIds().addAll(mvnArgs.getActiveProfileIds());
        request.getInactiveProfileIds().addAll(mvnArgs.getInactiveProfileIds());
        request.setUserProperties(System.getProperties());
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
