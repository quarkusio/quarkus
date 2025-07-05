package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tooling.GradleApplicationModelBuilder.clearFlag;
import static io.quarkus.gradle.tooling.GradleApplicationModelBuilder.isFlagOn;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.DefaultApplicationModel;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;
import io.quarkus.gradle.tooling.ProjectDescriptor;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;

public abstract class QuarkusApplicationModelTask extends DefaultTask {

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b100;
    /* @formatter:on */

    public static final String QUARKUS_PROJECT_DESCRIPTOR_ARTIFACT_TYPE = "quarkus-project-descriptor";

    @Internal
    public abstract RegularFileProperty getProjectBuildFile();

    @Inject
    public abstract ProjectLayout getLayout();

    /**
     * Used just to track original classpath as an input, since resolving quarkus classpath is kinda expensive,
     * and we don't want to do that if task is up-to-date
     */
    @CompileClasspath
    public abstract ConfigurableFileCollection getOriginalClasspath();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDeploymentResolvedWorkaround();

    @Nested
    public abstract QuarkusResolvedClasspath getPlatformConfiguration();

    @Nested
    public abstract QuarkusResolvedClasspath getAppClasspath();

    @Nested
    public abstract QuarkusResolvedClasspath getDeploymentClasspath();

    @Nested
    public abstract QuarkusPlatformInfo getPlatformInfo();

    @Input
    public abstract Property<LaunchMode> getLaunchMode();

    @Input
    public abstract Property<String> getTypeModel();

    /**
     * If any project task changes, we will invalidate this task anyway
     */
    @Input
    public abstract Property<DefaultProjectDescriptor> getProjectDescriptor();

    @OutputFile
    public abstract RegularFileProperty getApplicationModel();

    public QuarkusApplicationModelTask() {
        getProjectBuildFile().set(getProject().getBuildFile());
    }

    @TaskAction
    public void execute() throws IOException {
        final DefaultProjectDescriptor projectDescriptor = getProjectDescriptor().get();

        final ResolvedDependencyBuilder appArtifact = getProjectArtifact(projectDescriptor);

        final ApplicationModelBuilder modelBuilder = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .setPlatformImports(getPlatformInfo().resolvePlatformImports())
                .addReloadableWorkspaceModule(appArtifact.getKey());

        collectDependencies(getAppClasspath(), modelBuilder, projectDescriptor.getWorkspaceModule(), projectDescriptor);
        collectExtensionDependencies(getDeploymentClasspath(), modelBuilder);
        DefaultApplicationModel model = modelBuilder.build();
        ToolingUtils.serializeAppModel(model, getApplicationModel().get().getAsFile().toPath());
    }

    private ResolvedDependencyBuilder getProjectArtifact(DefaultProjectDescriptor projectDescriptor) {
        ModuleVersionIdentifier moduleVersion = getAppClasspath().getRoot().get().getModuleVersion();
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(moduleVersion.getGroup())
                .setArtifactId(moduleVersion.getName())
                .setVersion(moduleVersion.getVersion());

        WorkspaceModule.Mutable module = projectDescriptor.getWorkspaceModule();
        // TODO this is necessary for now to set the proper ID, since the group ID and the version don't have proper values in the descriptor
        module.setModuleId(
                WorkspaceModuleId.of(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getVersion()));

        var mainSources = module.getMainSources();
        if (mainSources != null) {
            final PathList.Builder paths = PathList.builder();
            collectDestinationDirs(module.getMainSources().getSourceDirs(), paths);
            collectDestinationDirs(module.getMainSources().getResourceDirs(), paths);
            appArtifact.setResolvedPaths(paths.build());
        } else {
            appArtifact.setResolvedPaths(PathList.empty());
        }

        return appArtifact.setWorkspaceModule(module);
    }

    private static void collectDestinationDirs(Collection<SourceDir> sources, final PathList.Builder paths) {
        for (SourceDir src : sources) {
            final Path path = src.getOutputDir();
            if (paths.contains(path) || !Files.exists(path)) {
                continue;
            }
            paths.add(path);
        }
    }

    private static void collectDependencies(QuarkusResolvedClasspath classpath, ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable wsModule, ProjectDescriptor projectDescriptor) {
        final Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifacts = classpath
                .resolvedArtifactsByComponentIdentifier();

        Set<File> alreadyCollectedFiles = new HashSet<>(artifacts.size());
        final Set<ModuleVersionIdentifier> processedModules = new HashSet<>();
        classpath.getRoot().get().getDependencies().forEach(d -> {
            if (d instanceof ResolvedDependencyResult resolved) {
                final byte flags = (byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS
                        | COLLECT_RELOADABLE_MODULES);
                collectDependencies(resolved, modelBuilder, artifacts, wsModule, alreadyCollectedFiles,
                        processedModules, flags, projectDescriptor);
            }
        });
        Set<File> fileDependencies = new HashSet<>(classpath.getAllResolvedFiles().getFiles());

        fileDependencies.removeAll(alreadyCollectedFiles);
        fileDependenciesExtractor(modelBuilder, fileDependencies);
    }

    private static void fileDependenciesExtractor(ApplicationModelBuilder modelBuilder, Set<File> fileDependencies) {
        // detect FS paths that are direct file dependencies and are not part of resolution graph
        for (File f : fileDependencies) {
            if (!f.exists()) {
                continue;
            }
            // here we are trying to represent a direct FS path dependency
            // as an artifact dependency
            // SHA1 hash is used to avoid long file names in the lib dir
            final String parentPath = f.getParent();
            final String group = HashUtil.sha1(parentPath == null ? f.getName() : parentPath);
            String name = f.getName();
            String type = ArtifactCoords.TYPE_JAR;
            if (!f.isDirectory()) {
                final int dot = f.getName().lastIndexOf('.');
                if (dot > 0) {
                    name = f.getName().substring(0, dot);
                    type = f.getName().substring(dot + 1);
                }
            }
            // hash could be a better way to represent the version
            final String version = String.valueOf(f.lastModified());
            final ResolvedDependencyBuilder artifactBuilder = ResolvedDependencyBuilder.newInstance()
                    .setGroupId(group)
                    .setArtifactId(name)
                    .setType(type)
                    .setVersion(version)
                    .setResolvedPath(f.toPath())
                    .setDirect(true)
                    .setRuntimeCp()
                    .setDeploymentCp();
            Utils.processQuarkusDependency(artifactBuilder, modelBuilder);
            modelBuilder.addDependency(artifactBuilder);
        }
    }

    private static void collectDependencies(
            ResolvedDependencyResult resolvedDependency,
            ApplicationModelBuilder modelBuilder,
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> resolvedArtifacts,
            WorkspaceModule.Mutable parentModule,
            Set<File> collectedArtifactFiles,
            Set<ModuleVersionIdentifier> processedModules,
            byte flags,
            ProjectDescriptor projectDescriptor) {
        final ModuleVersionIdentifier moduleId = getModuleVersion(resolvedDependency);
        if (!processedModules.add(moduleId)) {
            return;
        }
        var projectModule = projectDescriptor
                .getWorkspaceModuleOrNull(WorkspaceModuleId.of(moduleId.getGroup(), moduleId.getName(), moduleId.getVersion()));
        final List<QuarkusResolvedArtifact> artifacts = getResolvedModuleArtifacts(resolvedArtifacts,
                resolvedDependency.getSelected().getId());
        if (artifacts.isEmpty()) {
            final byte finalFlags = flags;
            resolvedDependency.getSelected().getDependencies().forEach((Consumer<DependencyResult>) dependencyResult -> {
                if (dependencyResult instanceof ResolvedDependencyResult result) {
                    collectDependencies(result, modelBuilder, resolvedArtifacts,
                            projectModule,
                            collectedArtifactFiles,
                            processedModules, finalFlags, projectDescriptor);
                }
            });
            return;
        }

        byte newFlags = flags;
        for (QuarkusResolvedArtifact artifact : artifacts) {
            collectedArtifactFiles.add(artifact.file);
            String classifier = resolveClassifier(moduleId, artifact.file);
            final ArtifactKey artifactKey = ArtifactKey.of(
                    moduleId.getGroup(),
                    moduleId.getName(),
                    classifier,
                    artifact.type);
            if (!isDependency(artifact)
                    || modelBuilder.getDependency(artifactKey) != null
                    || modelBuilder.getApplicationArtifact().getKey().equals(artifactKey)) {
                continue;
            }

            final ArtifactCoords depCoords = new GACTV(artifactKey, moduleId.getVersion());
            ResolvedDependencyBuilder depBuilder = ResolvedDependencyBuilder.newInstance()
                    .setCoords(depCoords)
                    .setRuntimeCp()
                    .setDeploymentCp()
                    .setResolvedPath(artifact.file.toPath())
                    .setWorkspaceModule(projectModule);
            if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                depBuilder.setDirect(true);
                newFlags = clearFlag(newFlags, COLLECT_DIRECT_DEPS);
            }
            if (parentModule != null) {
                parentModule.addDependency(new ArtifactDependency(depCoords));
            }

            if (Utils.processQuarkusDependency(depBuilder, modelBuilder)) {
                if (isFlagOn(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                    depBuilder.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                    newFlags = clearFlag(newFlags, COLLECT_TOP_EXTENSION_RUNTIME_NODES);
                }
                newFlags = clearFlag(newFlags, COLLECT_RELOADABLE_MODULES);
            }
            if (isFlagOn(flags, COLLECT_RELOADABLE_MODULES)) {
                if (projectModule != null) {
                    depBuilder.setFlags(DependencyFlags.RELOADABLE);
                    modelBuilder.addReloadableWorkspaceModule(artifactKey);
                } else {
                    newFlags = clearFlag(newFlags, COLLECT_RELOADABLE_MODULES);
                }
            }
            modelBuilder.addDependency(depBuilder);
        }

        flags = newFlags;
        for (DependencyResult dependency : resolvedDependency.getSelected().getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult result) {
                collectDependencies(result, modelBuilder, resolvedArtifacts, projectModule,
                        collectedArtifactFiles,
                        processedModules, flags, projectDescriptor);
            }
        }
    }

    private static ModuleVersionIdentifier getModuleVersion(ResolvedDependencyResult resolvedDependency) {
        return Objects.requireNonNull(resolvedDependency.getSelected().getModuleVersion());
    }

    private static boolean isDependency(QuarkusResolvedArtifact a) {
        return a.file.getName().endsWith(ArtifactCoords.TYPE_JAR)
                || a.file.getName().endsWith(".exe")
                || a.file.isDirectory();
    }

    private static void collectExtensionDependencies(QuarkusResolvedClasspath classpath, ApplicationModelBuilder modelBuilder) {
        Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifacts = classpath.resolvedArtifactsByComponentIdentifier();
        Set<ArtifactKey> alreadyVisited = new HashSet<>();
        classpath.getRoot().get().getDependencies().forEach(d -> {
            if (d instanceof ResolvedDependencyResult result) {
                collectExtensionDependencies(result, modelBuilder, artifacts, alreadyVisited);
            }
        });
    }

    private static void collectExtensionDependencies(
            ResolvedDependencyResult resolvedDependency,
            ApplicationModelBuilder modelBuilder,
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> resolvedArtifacts,
            Set<ArtifactKey> alreadyVisited) {
        List<QuarkusResolvedArtifact> artifacts = getResolvedModuleArtifacts(resolvedArtifacts,
                resolvedDependency.getSelected().getId());
        if (artifacts.isEmpty()) {
            return;
        }

        final ModuleVersionIdentifier moduleVersionIdentifier = getModuleVersion(resolvedDependency);
        for (QuarkusResolvedArtifact artifact : artifacts) {

            String classifier = resolveClassifier(moduleVersionIdentifier, artifact.file);
            ArtifactKey artifactKey = new GACT(moduleVersionIdentifier.getGroup(), moduleVersionIdentifier.getName(),
                    classifier,
                    artifact.type);
            if (!alreadyVisited.add(artifactKey)) {
                return;
            }

            ResolvedDependencyBuilder dep = modelBuilder.getDependency(artifactKey);
            if (dep == null) {
                ArtifactCoords artifactCoords = new GACTV(artifactKey, moduleVersionIdentifier.getVersion());
                dep = toDependency(artifactCoords, artifact.file);
                modelBuilder.addDependency(dep);
            }
            dep.setDeploymentCp();
            dep.clearFlag(DependencyFlags.RELOADABLE);

        }
        resolvedDependency.getSelected().getDependencies().forEach(d -> {
            if (d instanceof ResolvedDependencyResult result) {
                collectExtensionDependencies(result, modelBuilder, resolvedArtifacts, alreadyVisited);
            }
        });
    }

    private static List<QuarkusResolvedArtifact> getResolvedModuleArtifacts(
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifacts, ComponentIdentifier moduleId) {
        return artifacts.getOrDefault(moduleId, List.of());
    }

    private static String resolveClassifier(ModuleVersionIdentifier moduleVersionIdentifier, File file) {
        String artifactIdVersion = moduleVersionIdentifier.getVersion().isEmpty()
                || "unspecified".equals(moduleVersionIdentifier.getVersion())
                        ? moduleVersionIdentifier.getName()
                        : moduleVersionIdentifier.getName() + "-" + moduleVersionIdentifier.getVersion();
        if ((file.getName().endsWith(".jar") || file.getName().endsWith(".pom") || file.getName().endsWith(".exe"))
                && file.getName().startsWith(artifactIdVersion + "-")) {
            return file.getName().substring(artifactIdVersion.length() + 1, file.getName().length() - 4);
        }
        return "";
    }

    static ResolvedDependencyBuilder toDependency(ArtifactCoords artifactCoords, File file, int... flags) {
        int allFlags = 0;
        for (int f : flags) {
            allFlags |= f;
        }
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(artifactCoords)
                .setResolvedPaths(PathList.of(file.toPath()))
                .setFlags(allFlags);
    }

    public static abstract class QuarkusPlatformInfo {

        /**
         * Internal since we track defined dependencies via {@link QuarkusApplicationModelTask#getOriginalClasspath}
         */
        @Internal
        public abstract Property<ArtifactCollection> getResolvedArtifactCollection();

        private PlatformImportsImpl resolvePlatformImports() {
            final PlatformImportsImpl result = new PlatformImportsImpl();
            for (var artifact : getResolvedArtifactCollection().get().getArtifacts()) {
                var compId = ((ModuleComponentArtifactIdentifier) artifact.getId()).getComponentIdentifier();
                final String artifactId = artifact.getFile().getName();
                if (artifactId.endsWith(".json")) {
                    result.addPlatformDescriptor(compId.getGroup(), compId.getModuleIdentifier().getName(), compId.getVersion(),
                            "json", compId.getVersion());
                } else if (artifactId.endsWith(".properties")) {
                    try {
                        result.addPlatformProperties(compId.getGroup(), compId.getModuleIdentifier().getName(),
                                ArtifactCoords.DEFAULT_CLASSIFIER, "json", compId.getVersion(), artifact.getFile().toPath());
                    } catch (AppModelResolverException e) {
                        throw new RuntimeException("Failed to add platform properties " + artifact, e);
                    }
                }
            }
            return result;
        }

        public void configureFrom(Configuration configuration) {
            getResolvedArtifactCollection().set(configuration.getIncoming().getArtifacts());
        }
    }

    /**
     * See example https://docs.gradle.org/current/samples/sample_tasks_with_dependency_resolution_result_inputs.html,
     * to better understand how this works.
     */
    public static abstract class QuarkusResolvedClasspath {

        /**
         * Internal since we track defined dependencies via {@link QuarkusApplicationModelTask#getOriginalClasspath}
         */
        @Internal
        public abstract Property<ResolvedComponentResult> getRoot();

        /**
         * Internal since we track defined dependencies via {@link QuarkusApplicationModelTask#getOriginalClasspath}
         */
        @Internal
        public abstract Property<ArtifactCollection> getResolvedArtifactCollection();

        private FileCollection getAllResolvedFiles() {
            return getResolvedArtifactCollection().get().getArtifactFiles();
        }

        private Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> resolvedArtifactsByComponentIdentifier() {
            return getQuarkusResolvedArtifacts().stream()
                    .collect(Collectors.groupingBy(artifact -> artifact.getId().getComponentIdentifier()));
        }

        private List<QuarkusResolvedArtifact> getQuarkusResolvedArtifacts() {
            return getResolvedArtifactCollection().get().getArtifacts().stream()
                    .map(this::toResolvedArtifact)
                    .collect(toList());
        }

        private QuarkusResolvedArtifact toResolvedArtifact(ResolvedArtifactResult result) {
            String type = result.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
            return new QuarkusResolvedArtifact(result.getId(), result.getFile(), type);
        }

        public void configureFrom(Configuration configuration) {
            ResolvableDependencies resolvableDependencies = configuration.getIncoming();
            getRoot().set(resolvableDependencies.getResolutionResult().getRootComponent());
            getResolvedArtifactCollection().set(resolvableDependencies.getArtifacts());
        }
    }

    public static class QuarkusResolvedArtifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ComponentArtifactIdentifier id;
        private final String type;
        private final File file;

        public QuarkusResolvedArtifact(ComponentArtifactIdentifier id, File file, String type) {
            this.id = id;
            this.type = type;
            this.file = file;
        }

        public ComponentArtifactIdentifier getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public File getFile() {
            return file;
        }
    }

    public static class Utils {

        public static boolean processQuarkusDependency(ResolvedDependencyBuilder artifactBuilder,
                ApplicationModelBuilder modelBuilder) {
            for (Path artifactPath : artifactBuilder.getResolvedPaths()) {
                if (!Files.exists(artifactPath) || !artifactBuilder.getType().equals(ArtifactCoords.TYPE_JAR)) {
                    break;
                }
                if (Files.isDirectory(artifactPath)) {
                    return processQuarkusDir(artifactBuilder, artifactPath.resolve(BootstrapConstants.META_INF), modelBuilder);
                } else {
                    try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactPath)) {
                        return processQuarkusDir(artifactBuilder, artifactFs.getPath(BootstrapConstants.META_INF),
                                modelBuilder);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process " + artifactPath, e);
                    }
                }
            }
            return false;
        }

        private static boolean processQuarkusDir(ResolvedDependencyBuilder artifactBuilder, Path quarkusDir,
                ApplicationModelBuilder modelBuilder) {
            if (!Files.exists(quarkusDir)) {
                return false;
            }
            final Path quarkusDescr = quarkusDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
            if (!Files.exists(quarkusDescr)) {
                return false;
            }
            final Properties extProps = readDescriptor(quarkusDescr);
            if (extProps == null) {
                return false;
            }
            artifactBuilder.setRuntimeExtensionArtifact();
            modelBuilder.handleExtensionProperties(extProps, artifactBuilder.getKey());

            final String providesCapabilities = extProps.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
            if (providesCapabilities != null) {
                modelBuilder
                        .addExtensionCapabilities(
                                CapabilityContract.of(artifactBuilder.toGACTVString(), providesCapabilities, null));
            }
            return true;
        }

        private static Properties readDescriptor(final Path path) {
            final Properties rtProps;
            if (!Files.exists(path)) {
                // not a platform artifact
                return null;
            }
            rtProps = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                rtProps.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load extension description " + path, e);
            }
            return rtProps;
        }
    }
}
