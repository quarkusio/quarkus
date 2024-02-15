package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

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

    private final List<RawModule> moduleQueue = new ArrayList<>();
    private final Map<Path, Model> loadedPoms = new HashMap<>();

    private final Function<Path, Model> modelProvider;
    private final Map<GAV, Model> loadedModules = new HashMap<>();

    private final LocalWorkspace workspace = new LocalWorkspace();
    private final Path currentProjectPom;

    private ModelBuilder modelBuilder;
    private BootstrapModelResolver modelResolver;
    private ModelCache modelCache;
    private List<String> activeProfileIds;
    private List<String> inactiveProfileIds;
    private List<Profile> profiles;

    WorkspaceLoader(BootstrapMavenContext ctx, Path currentProjectPom, Function<Path, Model> modelProvider)
            throws BootstrapMavenException {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(currentProjectPom, BasicFileAttributes.class);
            this.currentProjectPom = fileAttributes.isDirectory() ? locateCurrentProjectPom(currentProjectPom)
                    : currentProjectPom;
        } catch (IOException e) {
            throw new IllegalArgumentException(currentProjectPom + " does not exist", e);
        }
        addModulePom(this.currentProjectPom);
        this.modelProvider = modelProvider == null ? pom -> null : modelProvider;

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
        }
        workspace.setBootstrapMavenContext(ctx);
    }

    private void addModulePom(Path pom) {
        if (pom != null) {
            moduleQueue.add(new RawModule(pom));
        }
    }

    void setWorkspaceRootPom(Path rootPom) {
        addModulePom(rootPom);
    }

    LocalProject load() throws BootstrapMavenException {
        final AtomicReference<LocalProject> currentProject = new AtomicReference<>();
        final Consumer<Model> processor;
        if (modelBuilder == null) {
            processor = rawModel -> {
                var project = new LocalProject(rawModel, workspace);
                if (currentProject.get() == null && project.getDir().equals(currentProjectPom.getParent())) {
                    currentProject.set(project);
                }
            };
        } else {
            processor = rawModel -> {
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
                    throw new RuntimeException("Failed to resolve the effective model for " + rawModel.getPomFile(), e);
                }
                if (currentProject.get() == null && project.getDir().equals(currentProjectPom.getParent())) {
                    currentProject.set(project);
                }
                for (var module : project.getModelBuildingResult().getEffectiveModel().getModules()) {
                    addModulePom(project.getDir().resolve(module).resolve(POM_XML));
                }
            };
        }

        int i = 0;
        while (i < moduleQueue.size()) {
            var newModules = new ArrayList<RawModule>();
            while (i < moduleQueue.size()) {
                loadModule(moduleQueue.get(i++), newModules);
            }
            for (var newModule : newModules) {
                newModule.process(processor);
            }
        }

        if (currentProject.get() == null) {
            throw new BootstrapMavenException("Failed to load project " + currentProjectPom);
        }
        return currentProject.get();
    }

    private void loadModule(RawModule rawModule, List<RawModule> newModules) {
        var moduleDir = rawModule.pom.getParent();
        if (moduleDir == null) {
            moduleDir = getFsRootDir();
        }
        if (loadedPoms.containsKey(moduleDir)) {
            return;
        }

        rawModule.model = modelProvider == null ? null : modelProvider.apply(moduleDir);
        if (rawModule.model == null) {
            rawModule.model = readModel(rawModule.pom);
        }
        loadedPoms.put(moduleDir, rawModule.model);
        if (rawModule.model == null) {
            return;
        }
        newModules.add(rawModule);

        var added = loadedModules.putIfAbsent(
                new GAV(ModelUtils.getGroupId(rawModule.model), rawModule.model.getArtifactId(),
                        ModelUtils.getVersion(rawModule.model)),
                rawModule.model);
        if (added != null) {
            return;
        }
        for (var module : rawModule.model.getModules()) {
            queueModule(rawModule.model.getProjectDirectory().toPath().resolve(module));
        }
        for (var profile : rawModule.model.getProfiles()) {
            for (var module : profile.getModules()) {
                queueModule(rawModule.model.getProjectDirectory().toPath().resolve(module));
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
                    var parent = new RawModule(parentPom);
                    rawModule.parent = parent;
                    moduleQueue.add(parent);
                }
            }
        }
    }

    private static Path getFsRootDir() {
        return Path.of("/");
    }

    private void queueModule(Path dir) {
        if (!loadedPoms.containsKey(dir)) {
            moduleQueue.add(new RawModule(dir.resolve(POM_XML)));
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
            return null;
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
            if (model != null) {
                consumer.accept(model);
            }
        }
    }
}
