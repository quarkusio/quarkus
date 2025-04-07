package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tooling.GradleApplicationModelBuilder.clearFlag;
import static io.quarkus.gradle.tooling.GradleApplicationModelBuilder.isFlagOn;
import static io.quarkus.maven.dependency.ArtifactCoords.DEFAULT_CLASSIFIER;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
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
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import com.google.common.base.Preconditions;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.workspace.descriptors.DefaultProjectDescriptor;
import io.quarkus.gradle.workspace.descriptors.ProjectDescriptor.TaskType;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;

@CacheableTask
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

    @Input
    public abstract Property<LaunchMode> getLaunchMode();

    @Input
    public abstract Property<String> getTypeModel();

    @Internal
    public abstract Property<PlatformImportsImpl> getPlatformImports();

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

    private void collectPlatforms(ResolvedDependencyResult resolvedDependency,
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifactsByCapability,
            PlatformImportsImpl platformImports) {
        List<QuarkusResolvedArtifact> artifacts = findArtifacts(resolvedDependency, artifactsByCapability);
        ModuleVersionIdentifier moduleVersionIdentifier = resolvedDependency.getSelected().getModuleVersion();
        for (QuarkusResolvedArtifact artifact : artifacts) {
            if (artifact != null && artifact.file.getName().endsWith(".properties")) {
                try {
                    platformImports.addPlatformProperties(moduleVersionIdentifier.getGroup(), moduleVersionIdentifier.getName(),
                            null, "properties", moduleVersionIdentifier.getVersion(), artifact.file.toPath());
                } catch (AppModelResolverException e) {
                    throw new GradleException("Failed to import platform properties " + artifact.file, e);
                }
            } else if (artifact != null && artifact.file.getName().endsWith(".json")) {
                platformImports.addPlatformDescriptor(moduleVersionIdentifier.getGroup(), moduleVersionIdentifier.getName(),
                        moduleVersionIdentifier.getVersion(), "json", moduleVersionIdentifier.getVersion());
            }
        }
    }

    @TaskAction
    public void execute() throws IOException {
        final ResolvedDependencyBuilder appArtifact = getProjectArtifact();
        Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifactsByCapability = getPlatformConfiguration()
                .resolvedArtifactsByComponentIdentifier();
        getPlatformConfiguration().getRoot().get().getDependencies().forEach(d -> {
            if (d instanceof ResolvedDependencyResult) {
                collectPlatforms((ResolvedDependencyResult) d, artifactsByCapability, getPlatformImports().get());
            }
        });
        final ApplicationModelBuilder modelBuilder = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .setPlatformImports(getPlatformImports().get())
                .addReloadableWorkspaceModule(appArtifact.getKey());

        collectDependencies(getAppClasspath(), modelBuilder, appArtifact.getWorkspaceModule().mutable());
        collectExtensionDependencies(getDeploymentClasspath(), modelBuilder);
        ToolingUtils.serializeAppModel(modelBuilder.build(), getApplicationModel().get().getAsFile().toPath());
    }

    private ResolvedDependencyBuilder getProjectArtifact() {
        ModuleVersionIdentifier moduleVersion = getAppClasspath().getRoot().get().getModuleVersion();
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(moduleVersion.getGroup())
                .setArtifactId(moduleVersion.getName())
                .setVersion(moduleVersion.getVersion());

        WorkspaceModule.Mutable mainModule = WorkspaceModule.builder()
                .setModuleId(new GAV(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getVersion()))
                .setModuleDir(getLayout().getProjectDirectory().getAsFile().toPath())
                .setBuildDir(getLayout().getBuildDirectory().getAsFile().get().toPath())
                .setBuildFile(getProjectBuildFile().getAsFile().get().toPath());

        DefaultProjectDescriptor projectDescriptor = getProjectDescriptor().get();

        initProjectModule(projectDescriptor, mainModule, ArtifactSources.MAIN, DEFAULT_CLASSIFIER,
                getProjectDescriptor().get().getSourceSetTasksRaw());
        if (getLaunchMode().get().isDevOrTest()) {
            initProjectModule(projectDescriptor, mainModule, ArtifactSources.TEST, "tests",
                    getProjectDescriptor().get().getSourceSetTasksRaw());
        }
        final PathList.Builder paths = PathList.builder();
        collectDestinationDirs(mainModule.getMainSources().getSourceDirs(), paths);
        collectDestinationDirs(mainModule.getMainSources().getResourceDirs(), paths);

        return appArtifact.setWorkspaceModule(mainModule).setResolvedPaths(paths.build());
    }

    private static void initProjectModule(DefaultProjectDescriptor projectDescriptor, WorkspaceModule.Mutable module,
            String sourceSetName, String classifier, Map<String, Set<String>> sourceSetTasksRaw) {
        List<SourceDir> sourceDirs = new ArrayList<>();
        List<SourceDir> resources = new ArrayList<>();
        Set<String> tasks = projectDescriptor.getTasksForSourceSet(sourceSetName.isEmpty()
                ? SourceSet.MAIN_SOURCE_SET_NAME
                : sourceSetName.equals("tests") ? SourceSet.TEST_SOURCE_SET_NAME : sourceSetName);
        for (String task : tasks) {
            TaskType type = projectDescriptor.getTaskType(task);
            Path source = Path.of(projectDescriptor.getTaskSource(task));
            Path destDir = Path.of(projectDescriptor.getTaskDestinationDir(task));
            if (type == TaskType.COMPILE) {
                sourceDirs.add(new DefaultSourceDir(source, destDir, null, Map.of("compiler", task)));
            } else if (type == TaskType.RESOURCES) {
                resources.add(new DefaultSourceDir(source, destDir, null));
            }
        }

        // Issue https://github.com/quarkusio/quarkus/issues/44384
        // no java sources are detected for compileJava before grpc configuration
        // so we need to verify if the destination source for the task exist and add it manually
        boolean containsJavaCompile = sourceDirs.stream()
                .anyMatch(sourceDir -> "compileJava".equals(sourceDir.getValue("compiler", String.class)));
        if (!containsJavaCompile && sourceSetTasksRaw.get("compileJava") != null) {

            sourceSetTasksRaw.get("compileJava").forEach(s -> {
                File output = new File(s);
                if (output.exists() && Objects.requireNonNull(output.listFiles()).length > 0) {
                    sourceDirs.add(
                            new DefaultSourceDir(output.toPath(), output.toPath(), null, Map.of("compiler", "compileJava")));
                }
            });

        }
        module.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resources));
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
            WorkspaceModule.Mutable wsModule) {
        Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifacts = classpath.resolvedArtifactsByComponentIdentifier();

        Set<File> alreadyCollectedFiles = new HashSet<>(artifacts.size());
        classpath.getRoot().get().getDependencies().forEach(d -> {
            if (d instanceof ResolvedDependencyResult) {
                byte flags = (byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS | COLLECT_RELOADABLE_MODULES);
                collectDependencies((ResolvedDependencyResult) d, modelBuilder, artifacts, wsModule, alreadyCollectedFiles,
                        new HashSet<>(), flags);
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
            Set<ArtifactKey> processedModules,
            byte flags) {
        WorkspaceModule.Mutable projectModule = null;
        List<QuarkusResolvedArtifact> artifacts = findArtifacts(resolvedDependency, resolvedArtifacts);
        if (artifacts.isEmpty()) {
            byte finalFlags = flags;
            resolvedDependency.getSelected().getDependencies().forEach((Consumer<DependencyResult>) dependencyResult -> {
                if (dependencyResult instanceof ResolvedDependencyResult) {
                    ModuleVersionIdentifier dependencyId = Preconditions
                            .checkNotNull(((ResolvedDependencyResult) dependencyResult).getSelected().getModuleVersion());
                    if (!processedModules.contains(new GACT(dependencyId.getGroup(), dependencyId.getName()))) {
                        collectDependencies((ResolvedDependencyResult) dependencyResult, modelBuilder, resolvedArtifacts,
                                projectModule,
                                collectedArtifactFiles,
                                processedModules, finalFlags);
                    }
                }
            });
            return;
        }

        ModuleVersionIdentifier moduleVersionIdentifier = Preconditions
                .checkNotNull(resolvedDependency.getSelected().getModuleVersion());

        for (QuarkusResolvedArtifact artifact : artifacts) {
            String classifier = resolveClassifier(moduleVersionIdentifier, artifact.file);
            ArtifactKey artifactKey = new GACT(
                    moduleVersionIdentifier.getGroup(),
                    moduleVersionIdentifier.getName(),
                    classifier,
                    artifact.type);
            if (!isDependency(artifact) || modelBuilder.getDependency(artifactKey) != null) {
                continue;
            }

            final ArtifactCoords depCoords = new GACTV(artifactKey, moduleVersionIdentifier.getVersion());
            ResolvedDependencyBuilder depBuilder = ResolvedDependencyBuilder.newInstance()
                    .setCoords(depCoords)
                    .setRuntimeCp()
                    .setDeploymentCp();
            if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                depBuilder.setDirect(true);
                flags = clearFlag(flags, COLLECT_DIRECT_DEPS);
            }
            if (parentModule != null) {
                parentModule.addDependency(new ArtifactDependency(depCoords));
            }

            PathCollection paths = null;

            depBuilder.setResolvedPaths(paths == null ? PathList.of(artifact.file.toPath()) : paths)
                    .setWorkspaceModule(projectModule);
            if (Utils.processQuarkusDependency(depBuilder, modelBuilder)) {
                if (isFlagOn(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                    depBuilder.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                    flags = clearFlag(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES);
                }
                flags = clearFlag(flags, COLLECT_RELOADABLE_MODULES);
            }
            if (!isFlagOn(flags, COLLECT_RELOADABLE_MODULES)) {
                depBuilder.clearFlag(DependencyFlags.RELOADABLE);
            }
            modelBuilder.addDependency(depBuilder);
            collectedArtifactFiles.add(artifact.file);
        }

        processedModules.add(ArtifactKey.ga(moduleVersionIdentifier.getGroup(), moduleVersionIdentifier.getName()));
        for (DependencyResult dependency : resolvedDependency.getSelected().getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult) {
                ModuleVersionIdentifier dependencyId = Preconditions
                        .checkNotNull(((ResolvedDependencyResult) dependency).getSelected().getModuleVersion());
                if (!processedModules.contains(new GACT(dependencyId.getGroup(), dependencyId.getName()))) {
                    collectDependencies((ResolvedDependencyResult) dependency, modelBuilder, resolvedArtifacts, projectModule,
                            collectedArtifactFiles,
                            processedModules, flags);
                }
            }
        }
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
            if (d instanceof ResolvedDependencyResult) {
                collectExtensionDependencies((ResolvedDependencyResult) d, modelBuilder, artifacts, alreadyVisited);
            }
        });
    }

    private static void collectExtensionDependencies(
            ResolvedDependencyResult resolvedDependency,
            ApplicationModelBuilder modelBuilder,
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> resolvedArtifacts,
            Set<ArtifactKey> alreadyVisited) {
        List<QuarkusResolvedArtifact> artifacts = findArtifacts(resolvedDependency, resolvedArtifacts);
        if (artifacts.isEmpty()) {
            return;
        }

        for (QuarkusResolvedArtifact artifact : artifacts) {
            ModuleVersionIdentifier moduleVersionIdentifier = Preconditions
                    .checkNotNull(resolvedDependency.getSelected().getModuleVersion());

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
            if (d instanceof ResolvedDependencyResult) {
                collectExtensionDependencies((ResolvedDependencyResult) d, modelBuilder, resolvedArtifacts, alreadyVisited);
            }
        });
    }

    private static List<QuarkusResolvedArtifact> findArtifacts(
            ResolvedDependencyResult resolvedDependency,
            Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> artifacts) {
        return artifacts.getOrDefault(resolvedDependency.getSelected().getId(), Collections.emptyList());
    }

    private static String resolveClassifier(ModuleVersionIdentifier moduleVersionIdentifier, File file) {
        String artifactIdVersion = moduleVersionIdentifier.getVersion().isEmpty()
                || "unspecified".equals(moduleVersionIdentifier.getVersion())
                        ? moduleVersionIdentifier.getName()
                        : moduleVersionIdentifier.getName() + "-" + moduleVersionIdentifier.getVersion();
        if ((file.getName().endsWith(".jar") || file.getName().endsWith(".pom") || file.getName().endsWith(".exe"))
                && file.getName().startsWith(artifactIdVersion + "-")) {
            int extensionLength = file.getName().endsWith(".exe") ? 4 : 4;
            return file.getName().substring(artifactIdVersion.length() + 1, file.getName().length() - extensionLength);
        }
        return "";
    }

    static ResolvedDependencyBuilder toDependency(ArtifactCoords artifactCoords, File file, int... flags) {
        int allFlags = 0;
        for (int f : flags) {
            allFlags |= f;
        }
        PathList paths = PathList.of(file.toPath());
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(artifactCoords)
                .setResolvedPaths(paths)
                .setFlags(allFlags);
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

        /**
         * TODO: Remove me
         */
        @Internal
        public abstract ConfigurableFileCollection getProjectDescriptors();

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
            File file = result.getFile();
            return new QuarkusResolvedArtifact(result.getId(), file, type);
        }

        public void configureFrom(Configuration configuration) {
            ResolvableDependencies resolvableDependencies = configuration.getIncoming();
            getRoot().set(resolvableDependencies.getResolutionResult().getRootComponent());
            getResolvedArtifactCollection().set(resolvableDependencies.getArtifacts());
            // TODO: Remove me, since we don't apply workspace plugin anymore, so there are no project descriptors
            getProjectDescriptors().setFrom(configuration.getIncoming().artifactView(viewConfiguration -> {
                // Project descriptors make sense only for projects
                viewConfiguration.withVariantReselection();
                viewConfiguration.componentFilter(component -> component instanceof ProjectComponentIdentifier);
                viewConfiguration.attributes(attributes -> attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                        QUARKUS_PROJECT_DESCRIPTOR_ARTIFACT_TYPE));
            }).getFiles());
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

    public static class QuarkusCapability {

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
