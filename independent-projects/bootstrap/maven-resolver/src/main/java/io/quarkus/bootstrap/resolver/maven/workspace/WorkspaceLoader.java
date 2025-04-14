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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
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
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;

public class WorkspaceLoader implements WorkspaceModelResolver, WorkspaceReader {

    private static final Logger log = Logger.getLogger(WorkspaceLoader.class);

    private static final String POM_XML = "pom.xml";

    private static final Model MISSING_MODEL = new Model();

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

    private final Deque<RawModule> moduleQueue = new ConcurrentLinkedDeque<>();
    private final Map<Path, Model> loadedPoms = new ConcurrentHashMap<>();

    private final Map<GAV, Model> loadedModules = new ConcurrentHashMap<>();

    private final LocalWorkspace workspace = new LocalWorkspace();
    private final Path currentProjectPom;
    private boolean warnOnFailingWsModules;

    private ModelBuilder modelBuilder;
    private BootstrapModelResolver modelResolver;
    private ModelCache modelCache;
    private List<String> activeProfileIds;
    private List<String> inactiveProfileIds;
    private List<Profile> profiles;

    WorkspaceLoader(BootstrapMavenContext ctx, Path currentProjectPom, Map<Path, Model> modelProvider)
            throws BootstrapMavenException {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(currentProjectPom, BasicFileAttributes.class);
            this.currentProjectPom = fileAttributes.isDirectory() ? locateCurrentProjectPom(currentProjectPom)
                    : currentProjectPom;
        } catch (IOException e) {
            throw new IllegalArgumentException(currentProjectPom + " does not exist", e);
        }
        if (modelProvider != null) {
            // queue all the provided POMs
            for (var e : modelProvider.entrySet()) {
                moduleQueue.push(new RawModule(e.getKey(), e.getValue()));
            }
        }
        // make sure the current project POM is queued
        if (modelProvider == null || !modelProvider.containsKey(this.currentProjectPom)) {
            addModulePom(this.currentProjectPom);
        }

        if (ctx != null && ctx.isEffectiveModelBuilder()) {
            modelBuilder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
            modelResolver = BootstrapModelResolver.newInstance(ctx, this);
            modelCache = new BootstrapModelCache(modelResolver.getSession());

            profiles = ctx.getActiveSettingsProfiles();
            final BootstrapMavenOptions cliOptions = ctx.getCliOptions();
            activeProfileIds = new ArrayList<>(profiles.size() + cliOptions.getActiveProfileIds().size());
            for (Profile p : profiles) {
                activeProfileIds.add(p.getId());
            }
            activeProfileIds.addAll(cliOptions.getActiveProfileIds());
            inactiveProfileIds = cliOptions.getInactiveProfileIds();
            warnOnFailingWsModules = ctx.isWarnOnFailingWorkspaceModules();
        }
        workspace.setBootstrapMavenContext(ctx);
    }

    private void addModulePom(Path pom) {
        if (pom != null) {
            moduleQueue.push(new RawModule(pom));
        }
    }

    void setWorkspaceRootPom(Path rootPom) {
        addModulePom(rootPom);
    }

    LocalProject load() throws BootstrapMavenException {
        final ConcurrentLinkedDeque<Exception> errors = new ConcurrentLinkedDeque<>();
        final AtomicReference<LocalProject> currentProjectRef = new AtomicReference<>();
        final Consumer<Model> modelProcessor = getModelProcessor(currentProjectRef);
        while (!moduleQueue.isEmpty()) {
            final ConcurrentLinkedDeque<RawModule> newModules = new ConcurrentLinkedDeque<>();
            while (!moduleQueue.isEmpty()) {
                final Phaser phaser = new Phaser(1);
                while (!moduleQueue.isEmpty()) {
                    phaser.register();
                    final RawModule module = moduleQueue.removeLast();
                    CompletableFuture.runAsync(() -> {
                        try {
                            loadModule(module, newModules);
                        } catch (Exception e) {
                            errors.add(e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
                phaser.arriveAndAwaitAdvance();
                assertNoErrors(errors);
            }
            for (var newModule : newModules) {
                newModule.process(modelProcessor);
            }
        }

        final LocalProject result = currentProjectRef.get();
        if (result == null) {
            throw new BootstrapMavenException("Failed to load project " + currentProjectPom);
        }
        return result;
    }

    private Consumer<Model> getModelProcessor(AtomicReference<LocalProject> currentProjectRef) {
        if (modelBuilder == null) {
            return rawModel -> {
                var project = new LocalProject(rawModel, workspace);
                if (currentProjectRef.get() == null && project.getDir().equals(currentProjectPom.getParent())) {
                    currentProjectRef.set(project);
                }
            };
        }
        return rawModel -> {
            var req = new DefaultModelBuildingRequest();
            req.setPomFile(rawModel.getPomFile());
            req.setModelResolver(modelResolver);
            req.setSystemProperties(System.getProperties());
            req.setUserProperties(System.getProperties());
            req.setModelCache(modelCache);
            req.setActiveProfileIds(activeProfileIds);
            req.setInactiveProfileIds(inactiveProfileIds);
            req.setProfiles(profiles);
            req.setRawModel(rawModel);
            req.setWorkspaceModelResolver(this);
            LocalProject project;
            try {
                project = new LocalProject(modelBuilder.build(req), workspace);
            } catch (Exception e) {
                if (warnOnFailingWsModules) {
                    log.warn("Failed to resolve effective model for " + rawModel.getPomFile(), e);
                    return;
                }
                throw new RuntimeException("Failed to resolve the effective model for " + rawModel.getPomFile(), e);
            }
            if (currentProjectRef.get() == null && project.getDir().equals(currentProjectPom.getParent())) {
                currentProjectRef.set(project);
            }
            for (var module : project.getModelBuildingResult().getEffectiveModel().getModules()) {
                addModulePom(project.getDir().resolve(module).resolve(POM_XML));
            }
        };
    }

    private void loadModule(RawModule rawModule, Collection<RawModule> newModules) {
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
                    rawModule.parent = new RawModule(parentPom);
                    moduleQueue.push(rawModule.parent);
                }
            }
        }
    }

    private static Path getFsRootDir() {
        return Path.of("/");
    }

    private void queueModule(Path dir) {
        if (!loadedPoms.containsKey(dir)) {
            moduleQueue.push(new RawModule(dir.resolve(POM_XML)));
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
                ? project.getModelBuildingResult().getEffectiveModel()
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

    private void assertNoErrors(Collection<Exception> errors) throws BootstrapMavenException {
        if (!errors.isEmpty()) {
            var sb = new StringBuilder("The following errors were encountered while loading the workspace:");
            log.error(sb);
            var i = 1;
            for (var error : errors) {
                var prefix = i++ + ")";
                log.error(prefix, error);
                sb.append(System.lineSeparator()).append(prefix).append(" ").append(error.getLocalizedMessage());
                for (var e : error.getStackTrace()) {
                    sb.append(System.lineSeparator());
                    for (int j = 0; j < prefix.length(); ++j) {
                        sb.append(" ");
                    }
                    sb.append("at ").append(e);
                    if (e.getClassName().contains("io.quarkus")) {
                        sb.append(System.lineSeparator());
                        for (int j = 0; j < prefix.length(); ++j) {
                            sb.append(" ");
                        }
                        sb.append("...");
                        break;
                    }
                }
            }
            throw new BootstrapMavenException(sb.toString());
        }
    }

    private static Model readModel(Path pom) {
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

    private static class RawModule {
        final Path pom;
        Model model;
        RawModule parent;
        boolean processed;

        private RawModule(Path pom) {
            this(null, pom);
        }

        private RawModule(RawModule parent, Path pom) {
            this.pom = pom.normalize().toAbsolutePath();
            this.parent = parent;
        }

        public RawModule(Path pom, Model model) {
            this.pom = pom;
            this.model = model;
        }

        private Path getModuleDir() {
            var moduleDir = pom.getParent();
            return moduleDir == null ? getFsRootDir() : moduleDir;
        }

        private Model getModel() {
            return model == null ? model = readModel(pom) : model;
        }

        private Path getParentPom() {
            if (model == null) {
                return null;
            }
            Path parentPom = null;
            final Parent parent = model.getParent();
            if (parent != null && parent.getRelativePath() != null && !parent.getRelativePath().isEmpty()) {
                parentPom = pom.getParent().resolve(parent.getRelativePath()).normalize();
                if (Files.isDirectory(parentPom)) {
                    parentPom = parentPom.resolve(POM_XML);
                }
            } else {
                final Path parentDir = pom.getParent().getParent();
                if (parentDir != null) {
                    parentPom = parentDir.resolve(POM_XML);
                }
            }
            return parentPom != null && Files.exists(parentPom) ? parentPom : null;
        }

        private void process(Consumer<Model> consumer) {
            if (processed) {
                return;
            }
            processed = true;
            if (parent != null) {
                parent.process(consumer);
            }
            if (model != null && model != MISSING_MODEL) {
                consumer.accept(model);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(pom);
        }
    }
}
