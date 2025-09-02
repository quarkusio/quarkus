package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.Result;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenModelBuilder implements ModelBuilder {

    private final ModelBuilder builder;
    private final BootstrapMavenContext ctx;
    // This mapping is particularly useful when POM files do not exactly match the Model present in the LocalWorkspace.
    // This may happen when Maven extensions manipulate the original POMs by changing versions, etc.
    private final Map<File, LocalProject> poms;

    public MavenModelBuilder(BootstrapMavenContext ctx) {
        builder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
        this.ctx = ctx;
        if (ctx != null && ctx.getWorkspace() != null) {
            final Collection<LocalProject> projects = ctx.getWorkspace().getProjects().values();
            final Map<File, LocalProject> tmp = new HashMap<>(projects.size());
            for (var p : projects) {
                tmp.put(p.getRawModel().getPomFile(), p);
            }
            poms = tmp;
        } else {
            poms = Map.of();
        }
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        final LocalProject lp = getLocalProjectOrNull(request);
        if (lp != null) {
            if (lp.getModelBuildingResult() != null) {
                return lp.getModelBuildingResult();
            }
            if (request.getRawModel() == null) {
                request.setRawModel(lp.getRawModel());
            }
            completeWorkspaceProjectBuildRequest(request);
            final LocalWorkspace workspace = lp.getWorkspace();
            if (workspace != null) {
                request.setWorkspaceModelResolver(workspace);
            }
        }
        return builder.build(request);
    }

    private LocalProject getLocalProjectOrNull(ModelBuildingRequest request) {
        if (request.getPomFile() != null) {
            final LocalProject lp = poms.get(request.getPomFile());
            if (lp != null) {
                return lp;
            }
        }

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

        if (requestModel != null && ctx.getWorkspace() != null) {
            return ctx.getWorkspace().getLocalProjectOrNull(ModelUtils.getGroupId(requestModel),
                    requestModel.getArtifactId(), ModelUtils.getRawVersion(requestModel));
        }
        return null;
    }

    private void completeWorkspaceProjectBuildRequest(ModelBuildingRequest request) {
        final Set<String> addedProfiles;
        final List<Profile> profiles = request.getProfiles();
        if (profiles.isEmpty()) {
            addedProfiles = Set.of();
        } else {
            addedProfiles = new HashSet<>(profiles.size());
            for (Profile p : profiles) {
                addedProfiles.add(p.getId());
            }
        }

        final List<Profile> activeSettingsProfiles;
        try {
            activeSettingsProfiles = ctx.getActiveSettingsProfiles();
        } catch (BootstrapMavenException e) {
            var requestModel = request.getRawModel();
            throw new RuntimeException("Failed to build model for " + ModelUtils.getGroupId(requestModel)
                    + ":" + requestModel.getArtifactId() + ":" + ModelUtils.getVersion(requestModel), e);
        }

        for (Profile p : activeSettingsProfiles) {
            if (!addedProfiles.contains(p.getId())) {
                profiles.add(p);
                request.getActiveProfileIds().add(p.getId());
            }
        }

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
