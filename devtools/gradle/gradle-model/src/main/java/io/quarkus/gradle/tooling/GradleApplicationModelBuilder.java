package io.quarkus.gradle.tooling;

import static io.quarkus.gradle.tooling.ToolingUtils.getClassesOutputDir;
import static io.quarkus.gradle.tooling.dependency.DependencyUtils.getArtifactCoords;
import static io.quarkus.gradle.tooling.dependency.DependencyUtils.getKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.composite.IncludedBuildInternal;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.LazySourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;

public class GradleApplicationModelBuilder implements ParameterizedToolingModelBuilder<ModelParameter> {

    private static final String MAIN_RESOURCES_OUTPUT = "build/resources/main";
    private static final String CLASSES_OUTPUT = "build/classes";

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b100;
    /* @formatter:on */

    private final TaskDependencyFactory taskDepFactory;

    public GradleApplicationModelBuilder() {
        taskDepFactory = null;
    }

    public GradleApplicationModelBuilder(TaskDependencyFactory taskDepFactory) {
        this.taskDepFactory = taskDepFactory;
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(ApplicationModel.class.getName());
    }

    @Override
    public Class<ModelParameter> getParameterType() {
        return ModelParameter.class;
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        final ModelParameterImpl modelParameter = new ModelParameterImpl();
        modelParameter.setMode(LaunchMode.DEVELOPMENT.toString());
        return buildAll(modelName, modelParameter, project);
    }

    @Override
    public Object buildAll(String modelName, ModelParameter parameter, Project project) {
        final LaunchMode mode = LaunchMode.valueOf(parameter.getMode());

        final ApplicationDeploymentClasspathBuilder classpathBuilder = new ApplicationDeploymentClasspathBuilder(project, mode,
                taskDepFactory);
        final Configuration classpathConfig = classpathBuilder.getRuntimeConfiguration();
        final Configuration deploymentConfig = classpathBuilder.getDeploymentConfiguration();
        final PlatformImports platformImports = classpathBuilder.getPlatformImports();

        boolean workspaceDiscovery = LaunchMode.DEVELOPMENT.equals(mode) || LaunchMode.TEST.equals(mode)
                || Boolean.parseBoolean(System.getProperty(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY));
        if (!workspaceDiscovery) {
            Object o = project.getProperties().get(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY);
            if (o != null) {
                workspaceDiscovery = Boolean.parseBoolean(o.toString());
            }
        }

        final ResolvedDependencyBuilder appArtifact = getProjectArtifact(project, workspaceDiscovery);
        final ApplicationModelBuilder modelBuilder = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addReloadableWorkspaceModule(appArtifact.getKey())
                .setPlatformImports(platformImports);

        collectDependencies(classpathConfig.getResolvedConfiguration(), classpathConfig.getIncoming(), workspaceDiscovery,
                project, modelBuilder, appArtifact.getWorkspaceModule().mutable());
        collectExtensionDependencies(project, deploymentConfig, modelBuilder);
        for (var dep : modelBuilder.getDependencies()) {
            if (dep.isRuntimeCp()) {
                dep.setDeploymentCp();
            }
        }
        addCompileOnly(project, classpathBuilder, modelBuilder);

        return modelBuilder.build();
    }

    private static void addCompileOnly(Project project, ApplicationDeploymentClasspathBuilder classpathBuilder,
            ApplicationModelBuilder modelBuilder) {
        var compileOnlyConfig = classpathBuilder.getCompileOnly();
        final List<org.gradle.api.artifacts.ResolvedDependency> queue = new ArrayList<>(
                compileOnlyConfig.getResolvedConfiguration().getFirstLevelModuleDependencies());
        for (int i = 0; i < queue.size(); ++i) {
            var d = queue.get(i);
            boolean skip = true;
            for (var a : d.getModuleArtifacts()) {
                if (!isDependency(a)) {
                    continue;
                }
                var moduleId = a.getModuleVersion().getId();
                var appDep = modelBuilder
                        .getDependency(ArtifactKey.of(moduleId.getGroup(), moduleId.getName(), a.getClassifier(), a.getType()));
                if (appDep == null) {
                    appDep = addArtifactDependency(project, modelBuilder, a);
                    appDep.clearFlag(DependencyFlags.DEPLOYMENT_CP);
                }
                if (!appDep.isFlagSet(DependencyFlags.COMPILE_ONLY)) {
                    skip = false;
                    appDep.setFlags(DependencyFlags.COMPILE_ONLY);
                }
            }
            if (!skip) {
                queue.addAll(d.getChildren());
            }
        }
    }

    public static ResolvedDependencyBuilder getProjectArtifact(Project project, boolean workspaceDiscovery) {
        final ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(project.getGroup().toString())
                .setArtifactId(project.getName())
                .setVersion(project.getVersion().toString());

        final SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        final WorkspaceModule.Mutable mainModule = WorkspaceModule.builder()
                .setModuleId(
                        WorkspaceModuleId.of(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getVersion()))
                .setModuleDir(project.getProjectDir().toPath())
                .setBuildDir(project.getLayout().getBuildDirectory().getAsFile().get().toPath())
                .setBuildFile(project.getBuildFile().toPath());

        initProjectModule(project, mainModule, sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME), ArtifactSources.MAIN);
        if (workspaceDiscovery) {
            appArtifact.setReloadable();
            final TaskCollection<Test> testTasks = project.getTasks().withType(Test.class);
            if (!testTasks.isEmpty()) {
                final Map<File, SourceSet> sourceSetsByClassesDir = new HashMap<>();
                sourceSets.forEach(s -> {
                    s.getOutput().getClassesDirs().forEach(d -> {
                        if (d.exists()) {
                            sourceSetsByClassesDir.put(d, s);
                        }
                    });
                });
                testTasks.forEach(t -> {
                    if (t.getEnabled()) {
                        t.getTestClassesDirs().forEach(d -> {
                            if (d.exists()) {
                                final SourceSet sourceSet = sourceSetsByClassesDir.remove(d);
                                if (sourceSet != null) {
                                    initProjectModule(project, mainModule, sourceSet,
                                            sourceSet.getName().equals(SourceSet.TEST_SOURCE_SET_NAME)
                                                    ? ArtifactSources.TEST
                                                    : sourceSet.getName());
                                }
                            }
                        });
                    }
                });
            }
        }

        final PathList.Builder paths = PathList.builder();
        collectDestinationDirs(mainModule.getMainSources().getSourceDirs(), paths);
        collectDestinationDirs(mainModule.getMainSources().getResourceDirs(), paths);

        return appArtifact.setWorkspaceModule(mainModule).setResolvedPaths(paths.build());
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

    private void collectExtensionDependencies(Project project, Configuration deploymentConfiguration,
            ApplicationModelBuilder modelBuilder) {
        final ResolvedConfiguration rc = deploymentConfiguration.getResolvedConfiguration();
        final Set<ArtifactKey> processedDeps = new HashSet<>();
        for (var dep : rc.getFirstLevelModuleDependencies()) {
            processDeploymentDependency(project, dep, modelBuilder, false, processedDeps);
        }
    }

    private static void processDeploymentDependency(Project project, ResolvedDependency resolvedDep,
            ApplicationModelBuilder modelBuilder, boolean clearReloadableFlag, Set<ArtifactKey> processedDeps) {
        final Set<ResolvedArtifact> resolvedArtifacts = resolvedDep.getModuleArtifacts();
        boolean processChildren = resolvedArtifacts.isEmpty();
        for (var a : resolvedArtifacts) {
            final ArtifactKey artifactKey = getKey(a);
            if (!processedDeps.add(artifactKey)) {
                continue;
            }
            processChildren = true;
            ResolvedDependencyBuilder dep = modelBuilder.getDependency(artifactKey);
            if (dep == null) {
                if (isApplicationRoot(modelBuilder, artifactKey)) {
                    // An application root artifact may be found among the dependencies in a could of cases:
                    // test fixtures in an application project and as a deployment module in an extension project
                    // running deployment module tests.
                    // In case of test fixtures, the root artifact does not have to be added to the model as a dependency,
                    // it can simply be skipped.
                    // In case of a deployment test, it has to be added as a dependency, since otherwise, the deployment
                    // module will appear to be missing.
                    // This part here looks like a hack but appears to work for both cases so far.
                    dep = modelBuilder.getApplicationArtifact();
                } else if (a.getId()
                        .getComponentIdentifier() instanceof ProjectComponentIdentifier projectComponentIdentifier) {
                    var includedBuild = ToolingUtils.includedBuild(project,
                            projectComponentIdentifier.getBuild().getBuildPath());
                    final Project projectDep;
                    if (includedBuild != null) {
                        projectDep = ToolingUtils.includedBuildProject((IncludedBuildInternal) includedBuild,
                                projectComponentIdentifier.getProjectPath());
                    } else {
                        projectDep = project.getRootProject().findProject(projectComponentIdentifier.getProjectPath());
                    }
                    Objects.requireNonNull(projectDep,
                            () -> "project " + projectComponentIdentifier.getProjectPath() + " should exist");
                    SourceSetContainer sourceSets = projectDep.getExtensions().getByType(SourceSetContainer.class);

                    SourceSet mainSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    if (mainSourceSet == null) {
                        // try Kotlin multiplatform
                        mainSourceSet = sourceSets.findByName("jvmMain");
                        if (mainSourceSet == null) {
                            var msg = new StringBuilder()
                                    .append("Failed to determine the main source set of ").append(projectDep.getPath());
                            var i = sourceSets.iterator();
                            if (!i.hasNext()) {
                                msg.append(" because the project does not have any source set");
                            } else {
                                msg.append(" among the following source sets: ").append(i.next().getName());
                                while (i.hasNext()) {
                                    msg.append(", ").append(i.next().getName());
                                }
                            }
                            throw new RuntimeException(msg.toString());
                        }
                    }
                    dep = toDependency(a, mainSourceSet);
                    modelBuilder.addDependency(dep);
                } else if (isDependency(a)) {
                    dep = toDependency(a);
                    modelBuilder.addDependency(dep);
                }
                if (dep != null) {
                    modelBuilder.addDependency(dep);
                    clearReloadableFlag = true;
                }
            }
            if (dep != null) {
                if (dep.isRuntimeExtensionArtifact()) {
                    clearReloadableFlag = true;
                }
                dep.setDeploymentCp();
                if (clearReloadableFlag && dep != modelBuilder.getApplicationArtifact()) {
                    dep.clearFlag(DependencyFlags.RELOADABLE);
                }
            }
        }
        if (processChildren) {
            for (var child : resolvedDep.getChildren()) {
                processDeploymentDependency(project, child, modelBuilder, clearReloadableFlag, processedDeps);
            }
        }
    }

    private static boolean isApplicationRoot(ApplicationModelBuilder modelBuilder, ArtifactKey artifactKey) {
        return modelBuilder.getApplicationArtifact().getKey().equals(artifactKey);
    }

    private static ResolvedDependencyBuilder addArtifactDependency(Project project, ApplicationModelBuilder modelBuilder,
            ResolvedArtifact a) {
        ResolvedDependencyBuilder dep = modelBuilder.getDependency(getKey(a));
        if (dep == null) {
            if (a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier projectComponentIdentifier) {
                var includedBuild = ToolingUtils.includedBuild(project, projectComponentIdentifier.getBuild().getBuildPath());
                final Project projectDep;
                if (includedBuild != null) {
                    projectDep = ToolingUtils.includedBuildProject((IncludedBuildInternal) includedBuild,
                            projectComponentIdentifier.getProjectPath());
                } else {
                    projectDep = project.getRootProject().findProject(projectComponentIdentifier.getProjectPath());
                }
                Objects.requireNonNull(projectDep,
                        () -> "project " + projectComponentIdentifier.getProjectPath() + " should exist");
                SourceSetContainer sourceSets = projectDep.getExtensions().getByType(SourceSetContainer.class);

                dep = toDependency(a, sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
                modelBuilder.addDependency(dep);
            } else if (isDependency(a)) {
                dep = toDependency(a);
                modelBuilder.addDependency(dep);
            }
        }
        if (dep != null) {
            dep.setDeploymentCp();
            dep.clearFlag(DependencyFlags.RELOADABLE);
        }
        return dep;
    }

    private void collectDependencies(ResolvedConfiguration configuration, ResolvableDependencies dependencies,
            boolean workspaceDiscovery, Project project, ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable wsModule) {

        final Set<File> artifactFiles = getArtifactFilesOrNull(configuration, dependencies);
        for (ResolvedDependency d : configuration.getFirstLevelModuleDependencies()) {
            collectDependencies(d, workspaceDiscovery, project, artifactFiles, modelBuilder, wsModule,
                    (byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS | COLLECT_RELOADABLE_MODULES));
        }

        if (artifactFiles != null) {
            // detect FS paths that aren't provided by the resolved artifacts
            for (File f : dependencies.getFiles().getFiles()) {
                if (artifactFiles.contains(f) || !f.exists()) {
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
                processQuarkusDependency(artifactBuilder, modelBuilder);
                modelBuilder.addDependency(artifactBuilder);
            }
        }
    }

    private static Set<File> getArtifactFilesOrNull(ResolvedConfiguration configuration, ResolvableDependencies dependencies) {
        final Set<ResolvedArtifact> resolvedArtifacts = configuration.getResolvedArtifacts();
        // if the number of artifacts is less than the number of files then probably
        // the project includes direct file dependencies
        return resolvedArtifacts.size() < dependencies.getFiles().getFiles().size()
                ? new HashSet<>(resolvedArtifacts.size())
                : null;
    }

    private void collectDependencies(org.gradle.api.artifacts.ResolvedDependency resolvedDep, boolean workspaceDiscovery,
            Project project, Set<File> artifactFiles, ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable parentModule,
            byte flags) {
        WorkspaceModule.Mutable projectModule = null;
        final Set<ResolvedArtifact> resolvedArtifacts = resolvedDep.getModuleArtifacts();
        boolean processChildren = resolvedArtifacts.isEmpty();
        for (ResolvedArtifact a : resolvedArtifacts) {
            if (!isDependency(a)) {
                continue;
            }
            final ArtifactKey artifactKey = getKey(a);
            if (isApplicationRoot(modelBuilder, artifactKey)) {
                continue;
            }
            var depBuilder = modelBuilder.getDependency(artifactKey);
            if (depBuilder == null) {
                processChildren = true;
                final ArtifactCoords depCoords = getArtifactCoords(a);
                depBuilder = ResolvedDependencyBuilder.newInstance()
                        .setCoords(depCoords)
                        .setRuntimeCp();
                if (parentModule != null) {
                    parentModule.addDependency(new ArtifactDependency(depCoords));
                }

                PathCollection paths = null;
                if (workspaceDiscovery && a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier compId) {
                    Project projectDep = project.getRootProject().findProject(compId.getProjectPath());

                    final String classifier = a.getClassifier();
                    if (classifier == null || classifier.isEmpty()) {
                        final IncludedBuild includedBuild = ToolingUtils.includedBuild(project.getRootProject(),
                                compId.getBuild().getBuildPath());
                        if (includedBuild != null) {
                            if (includedBuild instanceof IncludedBuildInternal ib) {
                                projectDep = ToolingUtils.includedBuildProject(ib, compId.getProjectPath());
                            }
                            if (projectDep != null) {
                                initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder);
                            } else {
                                final PathList.Builder pathBuilder = PathList.builder();
                                addSubstitutedProject(pathBuilder, includedBuild.getProjectDir());
                                paths = pathBuilder.build();
                            }
                        } else {
                            initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder);
                        }
                    } else {
                        initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder);
                    }
                }

                depBuilder.setResolvedPaths(paths == null ? PathList.of(a.getFile().toPath()) : paths);
                if (processQuarkusDependency(depBuilder, modelBuilder)) {
                    flags = clearFlag(flags, COLLECT_RELOADABLE_MODULES);
                }
                modelBuilder.addDependency(depBuilder);

                if (artifactFiles != null) {
                    artifactFiles.add(a.getFile());
                }
            }
            if (projectModule == null && depBuilder.getWorkspaceModule() != null) {
                projectModule = depBuilder.getWorkspaceModule().mutable();
            }
            if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                depBuilder.setDirect(true);
                flags = clearFlag(flags, COLLECT_DIRECT_DEPS);
            }
            if (depBuilder.isRuntimeExtensionArtifact()) {
                if (isFlagOn(flags, COLLECT_RELOADABLE_MODULES)) {
                    flags = clearFlag(flags, COLLECT_RELOADABLE_MODULES);
                    processChildren = true;
                }
                if (isFlagOn(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES)
                        && !depBuilder.isFlagSet(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)) {
                    depBuilder.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                    flags = clearFlag(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES);
                }
            }
            if (!isFlagOn(flags, COLLECT_RELOADABLE_MODULES)) {
                depBuilder.clearFlag(DependencyFlags.RELOADABLE);
            }
        }

        if (processChildren) {
            for (org.gradle.api.artifacts.ResolvedDependency child : resolvedDep.getChildren()) {
                collectDependencies(child, workspaceDiscovery, project, artifactFiles, modelBuilder, projectModule, flags);
            }
        }
    }

    private void initProjectModuleAndBuildPaths(final Project project,
            ResolvedArtifact resolvedArtifact, ApplicationModelBuilder appModel, final ResolvedDependencyBuilder appDep) {
        if (project == null) {
            System.err.println("Error: could not find project for " + resolvedArtifact.getId().getDisplayName());
            throw new IllegalStateException("Could not find project for " + resolvedArtifact.getId().getDisplayName());
        }
        appDep.setWorkspaceModule().setReloadable();

        if (appDep.getWorkspaceModule() == null) {
            final WorkspaceModule.Mutable projectModule = appModel.getOrCreateProjectModule(
                    WorkspaceModuleId.of(resolvedArtifact.getModuleVersion().getId().getGroup(), resolvedArtifact.getName(),
                            resolvedArtifact.getModuleVersion().getId().getVersion()),
                    project.getProjectDir(),
                    project.getLayout().getBuildDirectory().get().getAsFile())
                    .setBuildFile(project.getBuildFile().toPath());
            ProjectDescriptorBuilder.initSourceDirs(project, projectModule);
            appDep.setWorkspaceModule(projectModule);
        }

        appModel.addReloadableWorkspaceModule(appDep.getKey());
    }

    private boolean processQuarkusDependency(ResolvedDependencyBuilder artifactBuilder, ApplicationModelBuilder modelBuilder) {
        for (Path artifactPath : artifactBuilder.getResolvedPaths()) {
            if (!Files.exists(artifactPath) || !artifactBuilder.getType().equals(ArtifactCoords.TYPE_JAR)) {
                break;
            }
            if (Files.isDirectory(artifactPath)) {
                return processQuarkusDir(artifactBuilder, artifactPath.resolve(BootstrapConstants.META_INF), modelBuilder);
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactPath)) {
                    return processQuarkusDir(artifactBuilder, artifactFs.getPath(BootstrapConstants.META_INF), modelBuilder);
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

    private static void initProjectModule(Project project, WorkspaceModule.Mutable module, SourceSet sourceSet,
            String classifier) {
        if (sourceSet == null) {
            return;
        }

        final FileCollection allClassesDirs = sourceSet.getOutput().getClassesDirs();
        // some plugins do not add source directories to source sets and they may be missing from sourceSet.getAllJava()
        // see https://github.com/quarkusio/quarkus/issues/20755

        final List<SourceDir> sourceDirs = new ArrayList<>(1);
        project.getTasks().withType(AbstractCompile.class,
                t -> configureCompileTask(t.getSource(), t.getDestinationDirectory(), allClassesDirs, sourceDirs, t,
                        sourceSet));

        maybeConfigureKotlinJvmCompile(project, allClassesDirs, sourceDirs, sourceSet);

        final LinkedHashMap<File, Path> resourceDirs = new LinkedHashMap<>(1);
        final File resourcesOutputDir = sourceSet.getOutput().getResourcesDir();

        project.getTasks().withType(ProcessResources.class, t -> {
            if (!t.getEnabled()) {
                return;
            }
            final FileCollection source = t.getSource();
            if (source.isEmpty()) {
                return;
            }
            if (!t.getDestinationDir().equals(resourcesOutputDir)) {
                return;
            }
            final Path destDir = t.getDestinationDir().toPath();
            source.getAsFileTree().visit(a -> {
                // we are looking for the root dirs containing sources
                if (a.getRelativePath().getSegments().length == 1) {
                    final File srcDir = a.getFile().getParentFile();
                    resourceDirs.put(srcDir, destDir);
                }
            });
        });
        // there could be a task generating resources
        if (resourcesOutputDir.exists() && resourceDirs.isEmpty()) {
            sourceSet.getResources().getSrcDirs()
                    .forEach(srcDir -> resourceDirs.put(srcDir, resourcesOutputDir.toPath()));
        }
        final List<SourceDir> resources = new ArrayList<>(resourceDirs.size());
        for (Map.Entry<File, Path> e : resourceDirs.entrySet()) {
            resources.add(new LazySourceDir(e.getKey().toPath(), e.getValue(), null));
        }
        module.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resources));
    }

    private static void maybeConfigureKotlinJvmCompile(Project project, FileCollection allClassesDirs,
            List<SourceDir> sourceDirs, SourceSet sourceSet) {
        for (var task : project.getTasks()) {
            if (task.getName().contains("compileKotlin") && task.getEnabled()) {
                int originalSourceDirsSize = sourceDirs.size();

                // This "try/catch" is needed because of the way the "quarkus-cli" Gradle tests work. Without it, the tests fail.
                try {
                    Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool");
                    doConfigureKotlinJvmCompile(project, allClassesDirs, sourceDirs, sourceSet);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                // if the above failed, there could still be a KotlinCompile task that's not easily discoverable
                if (originalSourceDirsSize == sourceDirs.size()) {
                    final File outputDir = getClassesOutputDir(task);
                    if (outputDir != null && task.getInputs().getHasInputs()) {
                        task.getInputs().getSourceFiles().getAsFileTree().visit(visitor -> {
                            if (visitor.getRelativePath().getSegments().length == 1) {
                                sourceDirs.add(SourceDir.of(visitor.getFile().getParentFile().toPath(), outputDir.toPath()));
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

    private static void doConfigureKotlinJvmCompile(Project project, FileCollection allClassesDirs,
            List<SourceDir> sourceDirs, SourceSet sourceSet) {
        // Use KotlinJvmCompile.class in a separate method to prevent that maybeConfigureKotlinJvmCompile() runs into
        // a ClassNotFoundException due to actually using KotlinJvmCompile.class.
        project.getTasks().withType(KotlinCompileTool.class, t -> configureCompileTask(t.getSources().getAsFileTree(),
                t.getDestinationDirectory(), allClassesDirs, sourceDirs, t, sourceSet));
    }

    private static void configureCompileTask(FileTree sources, DirectoryProperty destinationDirectory,
            FileCollection allClassesDirs, List<SourceDir> sourceDirs, Task task, SourceSet sourceSet) {
        if (!task.getEnabled() || sources.isEmpty()) {
            return;
        }
        final File destDir = destinationDirectory.getAsFile().get();
        if (!allClassesDirs.contains(destDir)) {
            return;
        }
        sources.visit(visitor -> {
            // we are looking for the root dirs containing sources
            if (visitor.getRelativePath().getSegments().length == 1) {
                final File srcDir = visitor.getFile().getParentFile();
                sourceDirs.add(new LazySourceDir(srcDir.toPath(), destDir.toPath(),
                        findGeneratedSourceDir(destDir, sourceSet),
                        Map.of("compiler", task.getName())));
            }
        });
    }

    private static Path findGeneratedSourceDir(File destDir, SourceSet sourceSet) {
        // destDir appears to be build/classes/java/main
        if (destDir.getParentFile() == null) {
            return null;
        }
        String language = destDir.getParentFile().getName(); // java
        String sourceSetName = destDir.getName(); // main
        // find the corresponding generated sources, same pattern, but under build/generated/sources/annotationProcessor/java/main
        for (File generatedDir : sourceSet.getOutput().getGeneratedSourcesDirs().getFiles()) {
            if (generatedDir.getParentFile() == null) {
                continue;
            }
            if (generatedDir.getName().equals(sourceSetName)
                    && generatedDir.getParentFile().getName().equals(language)) {
                return generatedDir.toPath();
            }
        }
        return null;
    }

    private void addSubstitutedProject(PathList.Builder paths, File projectFile) {
        File mainResourceDirectory = new File(projectFile, MAIN_RESOURCES_OUTPUT);
        if (mainResourceDirectory.exists()) {
            paths.add(mainResourceDirectory.toPath());
        }
        File classesOutput = new File(projectFile, CLASSES_OUTPUT);
        File[] languageDirectories = classesOutput.listFiles();
        if (languageDirectories != null) {
            for (File languageDirectory : languageDirectories) {
                if (languageDirectory.isDirectory()) {
                    for (File sourceSet : languageDirectory.listFiles()) {
                        if (sourceSet.isDirectory() && sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
                            paths.add(sourceSet.toPath());
                        }
                    }
                }
            }
        }
    }

    public static boolean isFlagOn(byte walkingFlags, byte flag) {
        return (walkingFlags & flag) > 0;
    }

    public static byte clearFlag(byte flags, byte flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
        return flags;
    }

    private static boolean isDependency(ResolvedArtifact a) {
        return ArtifactCoords.TYPE_JAR.equalsIgnoreCase(a.getExtension()) || "exe".equalsIgnoreCase(a.getExtension()) ||
                a.getFile().isDirectory();
    }

    /**
     * Creates an instance of Dependency and associates it with the ResolvedArtifact's path
     */
    static ResolvedDependencyBuilder toDependency(ResolvedArtifact a, int... flags) {
        return toDependency(a, PathList.of(a.getFile().toPath()), null, flags);
    }

    static ResolvedDependencyBuilder toDependency(ResolvedArtifact a, SourceSet s) {
        PathList.Builder resolvedPathBuilder = PathList.builder();

        for (File classesDir : s.getOutput().getClassesDirs()) {
            if (classesDir.exists()) {
                resolvedPathBuilder.add(classesDir.toPath());
            }
        }
        File resourceDir = s.getOutput().getResourcesDir();
        if (resourceDir != null && resourceDir.exists()) {
            resolvedPathBuilder.add(resourceDir.toPath());
        }

        return ResolvedDependencyBuilder
                .newInstance()
                .setResolvedPaths(resolvedPathBuilder.build())
                .setCoords(getArtifactCoords(a));
    }

    static ResolvedDependencyBuilder toDependency(ResolvedArtifact a, PathCollection paths, DefaultWorkspaceModule module,
            int... flags) {
        int allFlags = 0;
        for (int f : flags) {
            allFlags |= f;
        }
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(getArtifactCoords(a))
                .setResolvedPaths(paths)
                .setWorkspaceModule(module)
                .setFlags(allFlags);
    }
}
