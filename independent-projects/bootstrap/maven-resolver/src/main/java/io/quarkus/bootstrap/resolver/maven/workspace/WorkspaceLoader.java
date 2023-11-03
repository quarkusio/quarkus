package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelBuilderFactory;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelResolver;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;

public class WorkspaceLoader implements WorkspaceModelResolver, WorkspaceReader {

    private static final Logger log = Logger.getLogger(WorkspaceLoader.class);

    private static final String POM_XML = "pom.xml";

    static final Model readModel(Path pom) throws BootstrapMavenException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (NoSuchFileException e) {
            // some projects may be missing pom.xml relying on Maven extensions (e.g. tycho-maven-plugin) to build them,
            // which we don't support in this workspace loader
            log.warn("Module(s) under " + pom.getParent() + " will be handled as thirdparty dependencies because " + pom
                    + " does not exist");
            return null;
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
    private Function<Path, Model> modelProvider;

    private ModelBuilder modelBuilder;
    private ModelResolver modelResolver;
    private ModelCache modelCache;
    private List<String> activeProfileIds;
    private List<String> inactiveProfileIds;
    private List<Profile> profiles;

    WorkspaceLoader(BootstrapMavenContext ctx, Path currentProjectPom, Function<Path, Model> modelProvider)
            throws BootstrapMavenException {
        this.modelProvider = modelProvider;
        if (ctx != null && ctx.isEffectiveModelBuilder()) {
            modelBuilder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
            modelResolver = BootstrapModelResolver.newInstance(ctx, this);
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
                rawModel(p);
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
        final Model rawModel = rawModel(pomFile);
        if (rawModel == null) {
            return null;
        }
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
            req.setRawModel(rawModel);
            req.setWorkspaceModelResolver(this);
            try {
                project = new LocalProject(modelBuilder.build(req), workspace);
            } catch (Exception e) {
                throw new BootstrapMavenException("Failed to resolve the effective model for " + pomFile, e);
            }
        } else {
            project = new LocalProject(rawModel, workspace);
        }
        projectCache.put(pomFile.getParent(), project);
        return project;
    }

    private Model rawModel(Path pomFile) throws BootstrapMavenException {
        var moduleDir = pomFile.getParent();
        if (moduleDir != null) {
            // the path might not be normalized, while the modelProvider below would typically recognize normalized absolute paths
            moduleDir = moduleDir.normalize().toAbsolutePath();
        }
        Model rawModel = rawModelCache.get(moduleDir);
        if (rawModel != null) {
            return rawModel;
        }
        rawModel = modelProvider == null ? null : modelProvider.apply(moduleDir);
        if (rawModel == null) {
            rawModel = readModel(pomFile);
        }
        rawModelCache.put(moduleDir, rawModel);
        return rawModel;
    }

    void setWorkspaceRootPom(Path rootPom) {
        this.workspaceRootPom = rootPom;
    }

    private LocalProject loadProject(Path projectPom, String skipModule) throws BootstrapMavenException {
        final Model rawModel = rawModel(projectPom);
        if (rawModel == null) {
            return null;
        }
        final LocalProject parentProject = loadParentProject(projectPom, rawModel);
        final LocalProject project = project(projectPom);
        if (project == null) {
            return null;
        }
        if (parentProject != null) {
            parentProject.modules.add(project);
        }
        loadProjectModules(project, skipModule);
        return project;
    }

    private LocalProject loadParentProject(Path projectPom, final Model rawModel) throws BootstrapMavenException {
        final Path parentPom = getParentPom(projectPom, rawModel);
        return parentPom == null || rawModelCache.containsKey(parentPom.getParent()) ? null
                : loadProject(parentPom, parentPom.getParent().relativize(projectPom.getParent()).toString());
    }

    private Path getParentPom(Path projectPom, Model rawModel) {
        if (rawModel == null) {
            return null;
        }
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
        final List<String> modules;
        if (project.getModelBuildingResult() == null) {
            var projectModel = project.getRawModel();
            List<String> combinedList = null;
            for (var profile : projectModel.getProfiles()) {
                if (!profile.getModules().isEmpty()) {
                    if (combinedList == null) {
                        combinedList = new ArrayList<>(projectModel.getModules());
                    }
                    combinedList.addAll(profile.getModules());
                }
            }
            modules = combinedList == null ? projectModel.getModules() : combinedList;
        } else {
            modules = project.getModelBuildingResult().getEffectiveModel().getModules();
        }
        if (!modules.isEmpty()) {
            for (String module : modules) {
                if (module.equals(skipModule)) {
                    continue;
                }
                final Path modulePom = project.getDir().resolve(module).resolve(POM_XML);
                // some modules use different parent POMs than those that referred to them as their modules
                // so make sure the parent project has been loaded, before resolving the effective model of the module
                loadParentProject(modulePom, rawModel(modulePom));
                final LocalProject childProject = project(modulePom);
                if (childProject != null) {
                    project.modules.add(loadProjectModules(childProject, null));
                }
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

    @Override
    public WorkspaceRepository getRepository() {
        return workspace.getRepository();
    }

    @Override
    public File findArtifact(Artifact artifact) {
        return workspace.findArtifact(artifact);
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return workspace.findVersions(artifact);
    }
}
