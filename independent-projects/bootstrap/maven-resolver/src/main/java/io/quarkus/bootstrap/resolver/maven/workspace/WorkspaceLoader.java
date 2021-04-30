package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelBuilderFactory;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelResolver;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;

public class WorkspaceLoader implements WorkspaceModelResolver {

    private static final String POM_XML = "pom.xml";

    static final Model readModel(Path pom) throws BootstrapMavenException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException e) {
            throw new BootstrapMavenException("Failed to read " + pom, e);
        }
    }

    static Path locateCurrentProjectPom(Path path, boolean required) throws BootstrapMavenException {
        Path p = path;
        while (p != null) {
            final Path pom = p.resolve(POM_XML);
            if (Files.exists(pom)) {
                return pom;
            }
            p = p.getParent();
        }
        if (required) {
            throw new BootstrapMavenException("Failed to locate project pom.xml for " + path);
        }
        return null;
    }

    private final LocalWorkspace workspace = new LocalWorkspace();
    private final Map<Path, Model> rawModelCache = new HashMap<>();
    private final Map<Path, LocalProject> projectCache = new HashMap<>();
    private final Path currentProjectPom;
    private Path workspaceRootPom;

    private ModelBuilder modelBuilder;
    private ModelResolver modelResolver;
    private ModelCache modelCache;
    private List<String> activeProfileIds;
    private List<String> inactiveProfileIds;
    private List<Profile> profiles;

    WorkspaceLoader(BootstrapMavenContext ctx, Path currentProjectPom) throws BootstrapMavenException {
        if (ctx != null && ctx.isEffectiveModelBuilder()) {
            modelBuilder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
            modelResolver = BootstrapModelResolver.newInstance(ctx, workspace);
            modelCache = new BootstrapModelCache(ctx.getRepositorySystemSession());

            profiles = ctx.getActiveSettingsProfiles();
            final BootstrapMavenOptions cliOptions = ctx.getCliOptions();
            activeProfileIds = new ArrayList<>(profiles.size() + cliOptions.getActiveProfileIds().size());
            for (Profile p : profiles) {
                activeProfileIds.add(p.getId());
            }
            activeProfileIds.addAll(cliOptions.getActiveProfileIds());
            inactiveProfileIds = cliOptions.getInactiveProfileIds();
        }
        workspace.setBootstrapMavenContext(ctx);
        this.currentProjectPom = isPom(currentProjectPom) ? currentProjectPom
                : locateCurrentProjectPom(currentProjectPom, true);
    }

    private boolean isPom(Path p) {
        if (Files.exists(p) && !Files.isDirectory(p)) {
            try {
                loadAndCacheRawModel(p);
                return true;
            } catch (BootstrapMavenException e) {
                // not a POM file
            }
        }
        return false;
    }

    private LocalProject project(Path pomFile) throws BootstrapMavenException {
        final LocalProject project = projectCache.get(pomFile.getParent());
        return project == null ? loadAndCacheProject(pomFile) : project;
    }

    private LocalProject loadAndCacheProject(Path pomFile) throws BootstrapMavenException {
        final Model cachedRawModel = rawModelCache.get(pomFile.getParent());
        final LocalProject project;
        if (modelBuilder != null) {
            ModelBuildingRequest req = new DefaultModelBuildingRequest();
            req.setPomFile(pomFile.toFile());
            req.setModelResolver(modelResolver);
            req.setSystemProperties(System.getProperties());
            req.setUserProperties(System.getProperties());
            req.setModelCache(modelCache);
            req.setActiveProfileIds(activeProfileIds);
            req.setInactiveProfileIds(inactiveProfileIds);
            req.setProfiles(profiles);
            req.setRawModel(cachedRawModel);
            req.setWorkspaceModelResolver(this);
            try {
                project = new LocalProject(modelBuilder.build(req), workspace);
            } catch (Exception e) {
                throw new BootstrapMavenException("Failed to resolve the effective model for " + pomFile, e);
            }
        } else {
            project = new LocalProject(cachedRawModel == null ? readModel(pomFile) : cachedRawModel, workspace);
        }
        projectCache.put(pomFile.getParent(), project);
        return project;
    }

    private Model rawModel(Path pomFile) throws BootstrapMavenException {
        final Model rawModel = rawModelCache.get(pomFile.getParent());
        return rawModel == null ? loadAndCacheRawModel(pomFile) : rawModel;
    }

    private Model loadAndCacheRawModel(Path pomFile) throws BootstrapMavenException {
        final Model rawModel = readModel(pomFile);
        rawModelCache.put(pomFile.getParent(), rawModel);
        return rawModel;
    }

    void setWorkspaceRootPom(Path rootPom) {
        this.workspaceRootPom = rootPom;
    }

    private LocalProject loadProject(final Path projectPom, String skipModule) throws BootstrapMavenException {
        final Model rawModel = rawModel(projectPom);

        final Path parentPom = getParentPom(projectPom, rawModel);
        final LocalProject parentProject = parentPom == null || rawModelCache.containsKey(parentPom.getParent()) ? null
                : loadProject(parentPom, parentPom.getParent().relativize(projectPom.getParent()).toString());

        final LocalProject project = project(projectPom);
        if (parentProject != null) {
            parentProject.modules.add(project);
        }
        loadProjectModules(project, skipModule);
        return project;
    }

    private Path getParentPom(Path projectPom, Model rawModel) {
        Path parentPom = null;
        final Path projectDir = projectPom.getParent();
        final Parent parent = rawModel.getParent();
        if (parent != null && parent.getRelativePath() != null && !parent.getRelativePath().isEmpty()) {
            parentPom = projectDir.resolve(parent.getRelativePath()).normalize();
            if (Files.isDirectory(parentPom)) {
                parentPom = parentPom.resolve(POM_XML);
            }
        } else {
            final Path parentDir = projectDir.getParent();
            if (parentDir != null) {
                parentPom = parentDir.resolve(POM_XML);
            }
        }
        return parentPom != null && Files.exists(parentPom) ? parentPom : null;
    }

    private LocalProject loadProjectModules(LocalProject project, String skipModule) throws BootstrapMavenException {
        final List<String> modules = project.getModelBuildingResult() == null ? project.getRawModel().getModules()
                : project.getModelBuildingResult().getEffectiveModel().getModules();
        if (!modules.isEmpty()) {
            for (String module : modules) {
                if (module.equals(skipModule)) {
                    continue;
                }
                final LocalProject childProject = project(project.getDir().resolve(module).resolve(POM_XML));
                project.modules.add(loadProjectModules(childProject, null));
            }
        }
        return project;
    }

    LocalProject load() throws BootstrapMavenException {
        if (workspaceRootPom != null) {
            loadProject(workspaceRootPom, null);
        }
        LocalProject currentProject = projectCache.get(currentProjectPom.getParent());
        if (currentProject == null) {
            currentProject = loadProject(currentProjectPom, null);
        }
        if (workspace != null) {
            workspace.setCurrentProject(currentProject);
        }
        return currentProject;
    }

    @Override
    public Model resolveRawModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        final LocalProject project = workspace.getProject(groupId, artifactId);
        // we are comparing the raw version here because in case of a CI-friendly version (e.g. ${revision}) the versionConstraint will be an expression
        return project != null && ModelUtils.getRawVersion(project.getRawModel()).equals(versionConstraint)
                ? project.getRawModel()
                : null;
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        final LocalProject project = workspace.getProject(groupId, artifactId);
        return project != null && project.getVersion().equals(versionConstraint)
                ? project.getModelBuildingResult().getEffectiveModel()
                : null;
    }
}
