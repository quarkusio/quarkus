package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelBuilderFactory;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelResolver;
import io.quarkus.bootstrap.resolver.maven.ModelResolutionTaskRunner;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;

public class WorkspaceLoader implements WorkspaceModelResolver, WorkspaceReader {

    private static final Logger log = Logger.getLogger(WorkspaceLoader.class);

    static final String POM_XML = "pom.xml";

    static final Model MISSING_MODEL = new Model();

    static Path getFsRootDir() {
        return Path.of("/");
    }

    static Model readModel(Path pom) {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (NoSuchFileException e) {
            // some projects may be missing pom.xml relying on Maven extensions (e.g. tycho-maven-plugin) to build them,
            // which we don't support in this workspace loader
            log.warn("Module(s) under " + pom.getParent() + " will be handled as thirdparty dependencies because " + pom
                    + " does not exist");
            return MISSING_MODEL;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load POM from " + pom, e);
        }
    }

    private static Path locateCurrentProjectPom(Path path) throws BootstrapMavenException {
        Path p = path;
        while (p != null) {
            final Path pom = p.resolve(POM_XML);
            if (Files.exists(pom)) {
                return pom;
            }
            p = p.getParent();
        }
        throw new BootstrapMavenException("Failed to locate project pom.xml for " + path);
    }

    private final Deque<WorkspaceModulePom> moduleQueue = new ConcurrentLinkedDeque<>();
    private final Map<Path, Model> loadedPoms = new ConcurrentHashMap<>();
    private final Map<GAV, Model> loadedModules = new ConcurrentHashMap<>();
    private final Consumer<WorkspaceModulePom> modelProcessor;

    private final LocalWorkspace workspace = new LocalWorkspace();
    private final Path currentProjectPom;
    private volatile LocalProject currentProject;

    WorkspaceLoader(BootstrapMavenContext ctx, Path currentProjectPom, List<WorkspaceModulePom> providedModules)
            throws BootstrapMavenException {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(currentProjectPom, BasicFileAttributes.class);
            this.currentProjectPom = fileAttributes.isDirectory() ? locateCurrentProjectPom(currentProjectPom)
                    : currentProjectPom;
        } catch (IOException e) {
            throw new IllegalArgumentException(currentProjectPom + " does not exist", e);
        }
        boolean queueCurrentPom = this.currentProjectPom != null;
        if (providedModules != null) {
            // queue all the provided POMs
            for (var e : providedModules) {
                if (queueCurrentPom && this.currentProjectPom.equals(e.pom)) {
                    queueCurrentPom = false;
                }
                moduleQueue.push(e);
            }
        }

        if (queueCurrentPom) {
            moduleQueue.push(new WorkspaceModulePom(this.currentProjectPom));
        }

        modelProcessor = getModelProcessor(ctx);
        workspace.setBootstrapMavenContext(ctx);
    }

    private void addModulePom(Path pom) {
        if (pom != null) {
            moduleQueue.push(new WorkspaceModulePom(pom));
        }
    }

    void setWorkspaceRootPom(Path rootPom) {
        addModulePom(rootPom);
    }

    LocalProject load() throws BootstrapMavenException {
        final ModelResolutionTaskRunner taskRunner = ModelResolutionTaskRunner.getNonBlockingTaskRunner();
        while (!moduleQueue.isEmpty()) {
            final ConcurrentLinkedDeque<WorkspaceModulePom> newModules = new ConcurrentLinkedDeque<>();
            while (!moduleQueue.isEmpty()) {
                while (!moduleQueue.isEmpty()) {
                    final WorkspaceModulePom module = moduleQueue.removeLast();
                    taskRunner.run(() -> loadModule(module, newModules));
                }
                taskRunner.waitForCompletion();
            }
            for (var newModule : newModules) {
                newModule.process(modelProcessor);
            }
        }

        if (currentProject == null) {
            throw new BootstrapMavenException("Failed to load project " + currentProjectPom);
        }
        return currentProject;
    }

    private Consumer<WorkspaceModulePom> getModelProcessor(BootstrapMavenContext ctx) throws BootstrapMavenException {
        if (ctx == null || !ctx.isEffectiveModelBuilder()) {
            return rawModule -> {
                var project = new LocalProject(rawModule.getModel(), rawModule.effectiveModel, workspace);
                if (currentProject == null && project.getDir().equals(currentProjectPom.getParent())) {
                    currentProject = project;
                }
            };
        }

        final ModelBuilder modelBuilder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
        final BootstrapModelResolver modelResolver = BootstrapModelResolver.newInstance(ctx, this);
        final ModelCache modelCache = new BootstrapModelCache(modelResolver.getSession());
        final List<Profile> profiles = ctx.getActiveSettingsProfiles();
        final BootstrapMavenOptions cliOptions = ctx.getCliOptions();
        final List<String> activeProfileIds = new ArrayList<>(profiles.size() + cliOptions.getActiveProfileIds().size());
        for (Profile p : profiles) {
            activeProfileIds.add(p.getId());
        }
        activeProfileIds.addAll(cliOptions.getActiveProfileIds());
        final List<String> inactiveProfileIds = cliOptions.getInactiveProfileIds();
        final boolean warnOnFailingWsModules = ctx.isWarnOnFailingWorkspaceModules();

        return rawModule -> {
            var req = new DefaultModelBuildingRequest();
            req.setPomFile(rawModule.getModel().getPomFile());
            req.setModelResolver(modelResolver);
            req.setSystemProperties(System.getProperties());
            req.setUserProperties(System.getProperties());
            req.setModelCache(modelCache);
            req.setActiveProfileIds(activeProfileIds);
            req.setInactiveProfileIds(inactiveProfileIds);
            req.setProfiles(profiles);
            req.setRawModel(rawModule.getModel());
            req.setWorkspaceModelResolver(this);
            LocalProject project;
            try {
                project = new LocalProject(modelBuilder.build(req), workspace);
            } catch (Exception e) {
                if (warnOnFailingWsModules) {
                    log.warn("Failed to resolve effective model for " + rawModule.getModel().getPomFile(), e);
                    return;
                }
                throw new RuntimeException("Failed to resolve the effective model for " + rawModule.getModel().getPomFile(), e);
            }
            if (currentProject == null && project.getDir().equals(currentProjectPom.getParent())) {
                currentProject = project;
            }
            for (var module : project.getEffectiveModel().getModules()) {
                addModulePom(project.getDir().resolve(module).resolve(POM_XML));
            }
        };
    }

    private void loadModule(WorkspaceModulePom rawModule, Collection<WorkspaceModulePom> newModules) {
        final Path moduleDir = rawModule.getModuleDir();
        if (loadedPoms.containsKey(moduleDir)) {
            return;
        }

        final Model model = rawModule.getModel();
        loadedPoms.put(moduleDir, model);
        if (model == MISSING_MODEL) {
            return;
        }

        final String rawVersion = ModelUtils.getRawVersion(model);
        final String version = ModelUtils.isUnresolvedVersion(rawVersion)
                ? ModelUtils.resolveVersion(rawVersion, model)
                : rawVersion;
        final Model existingModel = loadedModules.putIfAbsent(
                new GAV(ModelUtils.getGroupId(model), model.getArtifactId(), version),
                model);
        if (existingModel != null) {
            return;
        }
        newModules.add(rawModule);

        if (!rawVersion.equals(version)) {
            loadedModules.putIfAbsent(new GAV(ModelUtils.getGroupId(model), model.getArtifactId(), rawVersion), model);
        }

        for (var module : model.getModules()) {
            queueModule(model.getProjectDirectory().toPath().resolve(module));
        }
        for (var profile : model.getProfiles()) {
            for (var module : profile.getModules()) {
                queueModule(model.getProjectDirectory().toPath().resolve(module));
            }
        }
        if (rawModule.parent == null) {
            final Path parentPom = rawModule.getParentPom();
            if (parentPom != null) {
                var parentDir = parentPom.getParent();
                if (parentDir == null) {
                    parentDir = getFsRootDir();
                }
                if (!loadedPoms.containsKey(parentDir)) {
                    rawModule.parent = new WorkspaceModulePom(parentPom);
                    moduleQueue.push(rawModule.parent);
                }
            }
        }
    }

    private void queueModule(Path dir) {
        if (!loadedPoms.containsKey(dir)) {
            moduleQueue.push(new WorkspaceModulePom(dir.resolve(POM_XML)));
        }
    }

    @Override
    public Model resolveRawModel(String groupId, String artifactId, String versionConstraint) {
        return loadedModules.get(new GAV(groupId, artifactId, versionConstraint));
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint) {
        final LocalProject project = workspace.getProject(groupId, artifactId);
        return project != null && project.getVersion().equals(versionConstraint)
                ? project.getEffectiveModel()
                : null;
    }

    @Override
    public WorkspaceRepository getRepository() {
        return workspace.getRepository();
    }

    @Override
    public File findArtifact(Artifact artifact) {
        if (!ArtifactCoords.TYPE_POM.equals(artifact.getExtension())) {
            return null;
        }
        var model = loadedModules.get(new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        return model == null ? null : model.getPomFile();
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        var model = loadedModules.get(new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        return model == null ? List.of() : List.of(ModelUtils.getVersion(model));
    }
}
