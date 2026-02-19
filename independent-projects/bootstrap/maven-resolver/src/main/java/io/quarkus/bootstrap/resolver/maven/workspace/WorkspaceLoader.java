package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import org.apache.maven.api.xml.XmlService;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.v4.MavenStaxReader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelBuilderFactory;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelResolver;
import io.quarkus.bootstrap.resolver.maven.ModelResolutionTaskRunner;
import io.quarkus.bootstrap.resolver.maven.ModelResolutionTaskRunnerFactory;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;

public class WorkspaceLoader implements WorkspaceModelResolver, WorkspaceReader {

    private static final Logger log = Logger.getLogger(WorkspaceLoader.class);

    static final String POM_XML = "pom.xml";

    static final Model MISSING_MODEL = new Model();

    /**
     * Preloads a few classes related to POM parsing. This is to avoid classloading deadlocks when called
     * in the context of the FacadeClassLoader, which isn't parallel capable atm.
     */
    private static void preloadMavenXmlReaders() {
        new MavenStaxReader().getXMLInputFactory();
        try {
            // maven-xml was added as parent-first in quarkus-core, which helps with initializing the resolver in a test process,
            // here we use TCCL to make sure quarkus:dev boots
            //final Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(XmlService.class.getName());
            final Class<?> cls = RepositorySystem.class.getClassLoader().loadClass(XmlService.class.getName());
            ServiceLoader.load(cls).findFirst();
        } catch (ClassNotFoundException e) {
            // it's expected to load the class. If it doesn't, there is no reason to fail at this point though, since
            // this preload is a kind of hacky workaround anyway
        }
    }

    private static ModelResolutionTaskRunner getTaskRunner() {
        if (ModelResolutionTaskRunnerFactory.isDefaultRunnerBlocking()) {
            return ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();
        }
        preloadMavenXmlReaders();
        return ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner();
    }

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

    private final Deque<WorkspaceModulePom> loadQueue = new ConcurrentLinkedDeque<>();
    // Map key is the normalized absolute Path to the module directory
    private final Map<Path, WorkspaceModulePom> knownModules = new ConcurrentHashMap<>();
    private final Map<GAV, Model> loadedModules = new ConcurrentHashMap<>();
    private final Consumer<WorkspaceModulePom> loadedModelProcessor;

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
        boolean queueCurrentPom = true;
        if (providedModules != null) {
            // queue all the provided POMs
            for (var module : providedModules) {
                if (queueCurrentPom && this.currentProjectPom.equals(module.pom)) {
                    queueCurrentPom = false;
                }
                knownModules.put(module.getModuleDir(), module);
                loadQueue.add(module);
            }
        }

        if (queueCurrentPom) {
            WorkspaceModulePom module = new WorkspaceModulePom(this.currentProjectPom);
            knownModules.put(module.getModuleDir(), module);
            loadQueue.add(module);
        }

        loadedModelProcessor = getLoadedModelProcessor(ctx);
        workspace.setBootstrapMavenContext(ctx);
    }

    void setWorkspaceRootPom(Path rootPom) {
        WorkspaceModulePom rootModule = new WorkspaceModulePom(rootPom);
        knownModules.put(rootModule.getModuleDir(), rootModule);
        loadQueue.addLast(rootModule);
    }

    LocalProject load() throws BootstrapMavenException {
        final ModelResolutionTaskRunner taskRunner = getTaskRunner();
        while (!loadQueue.isEmpty()) {
            while (!loadQueue.isEmpty()) {
                while (!loadQueue.isEmpty()) {
                    final WorkspaceModulePom module = loadQueue.removeLast();
                    taskRunner.run(() -> loadModule(module));
                }
                taskRunner.waitForCompletion();
            }
            for (var module : knownModules.values()) {
                if (module.isLoaded()) {
                    module.process(loadedModelProcessor);
                }
            }
        }

        if (currentProject == null) {
            log.errorf("Failed to locate %s among the following loaded modules:", currentProjectPom);
            for (Path moduleDir : knownModules.keySet()) {
                log.error("- " + moduleDir);
            }
            throw new BootstrapMavenException("Failed to locate " + currentProjectPom + " in the workspace");
        }
        return currentProject;
    }

    private Consumer<WorkspaceModulePom> getLoadedModelProcessor(BootstrapMavenContext ctx) throws BootstrapMavenException {
        if (ctx == null || !ctx.isEffectiveModelBuilder()) {
            return this::processLoadedRawModel;
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
            final LocalProject project;
            try {
                project = new LocalProject(modelBuilder.build(req), workspace);
            } catch (Exception e) {
                if (warnOnFailingWsModules) {
                    log.warn("Failed to resolve effective model for " + rawModule.getModel().getPomFile(), e);
                    return;
                }
                throw new RuntimeException("Failed to resolve the effective model for " + rawModule.getModel().getPomFile(), e);
            }
            loadedModule(project);
            for (var module : project.getEffectiveModel().getModules()) {
                queueModule(project.getDir().resolve(module));
            }
        };
    }

    private void processLoadedRawModel(WorkspaceModulePom module) {
        loadedModule(new LocalProject(module.getResolvedGroupId(), module.getResolvedVersion(), module.getModel(),
                module.effectiveModel, workspace));
    }

    private void loadedModule(LocalProject project) {
        log.debugf("Loaded module from %s", project.getDir());
        if (currentProject == null && project.getDir().equals(currentProjectPom.getParent())) {
            currentProject = project;
        }
    }

    private void loadModule(WorkspaceModulePom module) {
        if (!module.isNew()) {
            return;
        }

        final Model model = module.getModel();
        if (model == MISSING_MODEL) {
            return;
        }

        if (module.parent == null) {
            final Path parentPom = module.getParentPom();
            if (parentPom != null) {
                var parentDir = parentPom.getParent();
                if (parentDir == null) {
                    parentDir = getFsRootDir();
                }
                module.parent = knownModules.computeIfAbsent(parentDir, dir -> queuePom(parentPom));
                if (module.parent.isNew() && module.isParentConfigured()) {
                    module.parent.thenLoad(module);
                }
            }
        }
        if (module.parent != null && module.parent.isNew() && module.isParentConfigured()) {
            // the parent still has not been loaded, once it's loaded, it will queue this module
            return;
        }

        final String version = module.getResolvedVersion();
        final Model existingModel = this.loadedModules.putIfAbsent(
                new GAV(module.getResolvedGroupId(), model.getArtifactId(), version),
                model);
        if (existingModel != null) {
            return;
        }
        final String rawVersion = ModelUtils.getRawVersionOrNull(model);
        if (rawVersion != null && !rawVersion.equals(version)) {
            this.loadedModules.putIfAbsent(new GAV(module.getResolvedGroupId(), model.getArtifactId(), rawVersion), model);
        }

        module.setLoaded();

        for (String modulePath : model.getModules()) {
            queueModule(model.getProjectDirectory().toPath().resolve(modulePath));
        }
        for (var profile : model.getProfiles()) {
            for (String modulePath : profile.getModules()) {
                queueModule(model.getProjectDirectory().toPath().resolve(modulePath));
            }
        }

        // queue dependencies
        // these may or may not be modules/subprojects of this POM
        var thenLoad = module.getThenLoad();
        while (!thenLoad.isEmpty()) {
            loadQueue.add(thenLoad.removeLast());
        }
    }

    private void queueModule(Path module) {
        Path normalizedModuleDir = module.normalize().toAbsolutePath();
        final Path pom;
        if (Files.isDirectory(normalizedModuleDir)) {
            pom = normalizedModuleDir.resolve(POM_XML);
        } else {
            // it is valid in Maven to point directly to the POM file in the <module> so we need to take that case into account
            pom = normalizedModuleDir;
            normalizedModuleDir = normalizedModuleDir.getParent() != null ? normalizedModuleDir.getParent() : getFsRootDir();
        }
        knownModules.computeIfAbsent(normalizedModuleDir, dir -> queuePom(pom));
    }

    private WorkspaceModulePom queuePom(Path pomFile) {
        var module = new WorkspaceModulePom(pomFile);
        loadQueue.add(module);
        return module;
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
