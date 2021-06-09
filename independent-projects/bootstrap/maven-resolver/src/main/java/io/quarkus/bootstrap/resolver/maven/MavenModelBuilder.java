package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.Result;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenModelBuilder implements ModelBuilder {

    private final ModelBuilder builder;
    private final BootstrapMavenContext ctx;

    public MavenModelBuilder(BootstrapMavenContext ctx) {
        builder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
        this.ctx = ctx;
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        final LocalWorkspace workspace = ctx.getWorkspace();
        if (workspace != null) {
            request.setWorkspaceModelResolver(workspace);
            final Model requestModel = getModel(request);
            if (requestModel != null) {
                final Artifact artifact = new DefaultArtifact(ModelUtils.getGroupId(requestModel), requestModel.getArtifactId(),
                        null, "pom",
                        ModelUtils.getVersion(requestModel));
                if (workspace.findArtifact(artifact) != null) {
                    final ModelBuildingResult result = workspace
                            .getProject(artifact.getGroupId(), artifact.getArtifactId()).getModelBuildingResult();
                    if (result != null) {
                        return result;
                    }
                    try {
                        completeWorkspaceProjectBuildRequest(request);
                    } catch (BootstrapMavenException e) {
                        throw new RuntimeException("Failed to build model for " + ModelUtils.getGroupId(requestModel)
                                + ":" + requestModel.getArtifactId() + ":" + ModelUtils.getVersion(requestModel), e);
                    }
                }
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
                    if (request.getPomFile() != null) {
                        requestModel.setPomFile(request.getPomFile());
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return requestModel;
    }

    private void completeWorkspaceProjectBuildRequest(ModelBuildingRequest request) throws BootstrapMavenException {
        final Set<String> addedProfiles = new HashSet<>();
        final List<Profile> profiles = request.getProfiles();
        profiles.forEach(p -> addedProfiles.add(p.getId()));

        final List<Profile> activeSettingsProfiles = ctx.getActiveSettingsProfiles();
        activeSettingsProfiles.forEach(p -> {
            if (!addedProfiles.contains(p.getId())) {
                profiles.add(p);
                request.getActiveProfileIds().add(p.getId());
            }
        });

        final BootstrapMavenOptions cliOptions = ctx.getCliOptions();
        request.getActiveProfileIds().addAll(cliOptions.getActiveProfileIds());
        request.getInactiveProfileIds().addAll(cliOptions.getInactiveProfileIds());
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
