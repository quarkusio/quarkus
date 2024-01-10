package io.quarkus.gradle.tooling;

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
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.composite.IncludedBuildInternal;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependency;
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

        final ApplicationDeploymentClasspathBuilder classpathBuilder = new ApplicationDeploymentClasspathBuilder(project,
                mode);
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

        final ResolvedDependency appArtifact = getProjectArtifact(project, workspaceDiscovery);
        final ApplicationModelBuilder modelBuilder = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addReloadableWorkspaceModule(appArtifact.getKey())
                .setPlatformImports(platformImports);

        collectDependencies(classpathConfig.getResolvedConfiguration(), workspaceDiscovery,
                project, modelBuilder, appArtifact.getWorkspaceModule().mutable());
        collectExtensionDependencies(project, deploymentConfig, modelBuilder);
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
                var key = ArtifactKey.of(moduleId.getGroup(), moduleId.getName(), a.getClassifier(), a.getType());
                var appDep = modelBuilder.getDependency(key);
                if (appDep == null) {
                    addArtifactDependency(project, modelBuilder, a);
                    appDep = modelBuilder.getDependency(key);
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

    public static ResolvedDependency getProjectArtifact(Project project, boolean workspaceDiscovery) {
        final ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(project.getGroup().toString())
                .setArtifactId(project.getName())
                .setVersion(project.getVersion().toString());

        final SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        final WorkspaceModule.Mutable mainModule = WorkspaceModule.builder()
                .setModuleId(new GAV(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getVersion()))
                .setModuleDir(project.getProjectDir().toPath())
                .setBuildDir(project.getBuildDir().toPath())
                .setBuildFile(project.getBuildFile().toPath());

        initProjectModule(project, mainModule, sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME), ArtifactSources.MAIN);
        if (workspaceDiscovery) {
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

        return appArtifact.setWorkspaceModule(mainModule).setResolvedPaths(paths.build()).build();
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
        for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
            addArtifactDependency(project, modelBuilder, a);
        }
    }

    private static void addArtifactDependency(Project project, ApplicationModelBuilder modelBuilder, ResolvedArtifact a) {
        if (a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
            ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier) a.getId()
                    .getComponentIdentifier();
            var includedBuild = ToolingUtils.includedBuild(project, projectComponentIdentifier);
            final Project projectDep;
            if (includedBuild != null) {
                projectDep = ToolingUtils.includedBuildProject((IncludedBuildInternal) includedBuild,
                        projectComponentIdentifier);
            } else {
                projectDep = project.getRootProject().findProject(projectComponentIdentifier.getProjectPath());
            }
            Objects.requireNonNull(projectDep,
                    () -> "project " + projectComponentIdentifier.getProjectPath() + " should exist");
            SourceSetContainer sourceSets = projectDep.getExtensions().getByType(SourceSetContainer.class);

            SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            ResolvedDependencyBuilder dep = modelBuilder.getDependency(
                    toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier()));
            if (dep == null) {
                dep = toDependency(a, mainSourceSet);
                modelBuilder.addDependency(dep);
            }
            dep.setDeploymentCp();
            dep.clearFlag(DependencyFlags.RELOADABLE);
        } else if (isDependency(a)) {
            ResolvedDependencyBuilder dep = modelBuilder.getDependency(
                    toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier()));
            if (dep == null) {
                dep = toDependency(a);
                modelBuilder.addDependency(dep);
            }
            dep.setDeploymentCp();
            dep.clearFlag(DependencyFlags.RELOADABLE);
        }
    }

    private void collectDependencies(ResolvedConfiguration configuration,
            boolean workspaceDiscovery, Project project, ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable wsModule) {

        final Set<ResolvedArtifact> resolvedArtifacts = configuration.getResolvedArtifacts();
        // if the number of artifacts is less than the number of files then probably
        // the project includes direct file dependencies
        final Set<File> artifactFiles = resolvedArtifacts.size() < configuration.getFiles().size()
                ? new HashSet<>(resolvedArtifacts.size())
                : null;

        configuration.getFirstLevelModuleDependencies()
                .forEach(d -> {
                    collectDependencies(d, workspaceDiscovery, project, artifactFiles, new HashSet<>(),
                            modelBuilder,
                            wsModule,
                            (byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS | COLLECT_RELOADABLE_MODULES));
                });

        if (artifactFiles != null) {
            // detect FS paths that aren't provided by the resolved artifacts
            for (File f : configuration.getFiles()) {
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

    private void collectDependencies(org.gradle.api.artifacts.ResolvedDependency resolvedDep, boolean workspaceDiscovery,
            Project project, Set<File> artifactFiles, Set<ArtifactKey> processedModules, ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable parentModule,
            byte flags) {
        WorkspaceModule.Mutable projectModule = null;
        for (ResolvedArtifact a : resolvedDep.getModuleArtifacts()) {
            final ArtifactKey artifactKey = toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(),
                    a.getClassifier());
            if (!isDependency(a)) {
                continue;
            }
            var depBuilder = modelBuilder.getDependency(artifactKey);
            if (depBuilder != null) {
                if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                    depBuilder.setDirect(true);
                }
                continue;
            }
            final ArtifactCoords depCoords = toArtifactCoords(a);
            depBuilder = ResolvedDependencyBuilder.newInstance()
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
            if (workspaceDiscovery && a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {

                Project projectDep = project.getRootProject().findProject(
                        ((ProjectComponentIdentifier) a.getId().getComponentIdentifier()).getProjectPath());
                SourceSetContainer sourceSets = projectDep == null ? null
                        : projectDep.getExtensions().findByType(SourceSetContainer.class);

                final String classifier = a.getClassifier();
                if (classifier == null || classifier.isEmpty()) {
                    final IncludedBuild includedBuild = ToolingUtils.includedBuild(project.getRootProject(),
                            (ProjectComponentIdentifier) a.getId().getComponentIdentifier());
                    if (includedBuild != null) {
                        final PathList.Builder pathBuilder = PathList.builder();

                        if (includedBuild instanceof IncludedBuildInternal) {
                            projectDep = ToolingUtils.includedBuildProject((IncludedBuildInternal) includedBuild,
                                    (ProjectComponentIdentifier) a.getId().getComponentIdentifier());
                        }
                        if (projectDep != null) {
                            projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                    pathBuilder, SourceSet.MAIN_SOURCE_SET_NAME, false);
                            addSubstitutedProject(pathBuilder, projectDep.getProjectDir());
                        } else {
                            addSubstitutedProject(pathBuilder, includedBuild.getProjectDir());
                        }
                        paths = pathBuilder.build();
                    } else if (sourceSets != null) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                pathBuilder, SourceSet.MAIN_SOURCE_SET_NAME, false);
                        paths = pathBuilder.build();
                    }
                } else if (sourceSets != null) {
                    if (SourceSet.TEST_SOURCE_SET_NAME.equals(classifier)) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                pathBuilder, SourceSet.TEST_SOURCE_SET_NAME, true);
                        paths = pathBuilder.build();
                    } else if ("test-fixtures".equals(classifier)) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                pathBuilder, "testFixtures", true);
                        paths = pathBuilder.build();
                    }
                }
            }

            depBuilder.setResolvedPaths(paths == null ? PathList.of(a.getFile().toPath()) : paths)
                    .setWorkspaceModule(projectModule);
            if (processQuarkusDependency(depBuilder, modelBuilder)) {
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

            if (artifactFiles != null) {
                artifactFiles.add(a.getFile());
            }
        }

        processedModules.add(ArtifactKey.ga(resolvedDep.getModuleGroup(), resolvedDep.getModuleName()));
        for (org.gradle.api.artifacts.ResolvedDependency child : resolvedDep.getChildren()) {
            if (!processedModules.contains(new GACT(child.getModuleGroup(), child.getModuleName()))) {
                collectDependencies(child, workspaceDiscovery, project, artifactFiles, processedModules,
                        modelBuilder, projectModule, flags);
            }
        }
    }

    private static String toNonNullClassifier(String resolvedClassifier) {
        return resolvedClassifier == null ? ArtifactCoords.DEFAULT_CLASSIFIER : resolvedClassifier;
    }

    private WorkspaceModule.Mutable initProjectModuleAndBuildPaths(final Project project,
            ResolvedArtifact resolvedArtifact, ApplicationModelBuilder appModel, final ResolvedDependencyBuilder appDep,
            PathList.Builder buildPaths, String sourceName, boolean test) {

        appDep.setWorkspaceModule().setReloadable();

        final WorkspaceModule.Mutable projectModule = appModel.getOrCreateProjectModule(
                new GAV(resolvedArtifact.getModuleVersion().getId().getGroup(), resolvedArtifact.getName(),
                        resolvedArtifact.getModuleVersion().getId().getVersion()),
                project.getProjectDir(),
                project.getBuildDir())
                .setBuildFile(project.getBuildFile().toPath());

        final String classifier = toNonNullClassifier(resolvedArtifact.getClassifier());
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        initProjectModule(project, projectModule, sourceSets.findByName(sourceName), classifier);

        collectDestinationDirs(projectModule.getSources(classifier).getSourceDirs(), buildPaths);
        collectDestinationDirs(projectModule.getSources(classifier).getResourceDirs(), buildPaths);

        appModel.addReloadableWorkspaceModule(
                ArtifactKey.of(resolvedArtifact.getModuleVersion().getId().getGroup(), resolvedArtifact.getName(), classifier,
                        ArtifactCoords.TYPE_JAR));
        return projectModule;
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
        final String extensionCoords = artifactBuilder.toGACTVString();
        modelBuilder.handleExtensionProperties(extProps, extensionCoords);

        final String providesCapabilities = extProps.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
        if (providesCapabilities != null) {
            modelBuilder
                    .addExtensionCapabilities(CapabilityContract.of(extensionCoords, providesCapabilities, null));
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
                t -> configureCompileTask(t.getSource(), t.getDestinationDirectory(), allClassesDirs, sourceDirs, t));

        maybeConfigureKotlinJvmCompile(project, allClassesDirs, sourceDirs);

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
            resources.add(new DefaultSourceDir(e.getKey().toPath(), e.getValue()));
        }
        module.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resources));
    }

    private static void maybeConfigureKotlinJvmCompile(Project project, FileCollection allClassesDirs,
            List<SourceDir> sourceDirs) {
        // This "try/catch" is needed because of the way the "quarkus-cli" Gradle tests work. Without it, the tests fail.
        try {
            Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile");
            doConfigureKotlinJvmCompile(project, allClassesDirs, sourceDirs);
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    private static void doConfigureKotlinJvmCompile(Project project, FileCollection allClassesDirs,
            List<SourceDir> sourceDirs) {
        // Use KotlinJvmCompile.class in a separate method to prevent that maybeConfigureKotlinJvmCompile() runs into
        // a ClassNotFoundException due to actually using KotlinJvmCompile.class.
        project.getTasks().withType(KotlinJvmCompile.class, t -> configureCompileTask(t.getSources().getAsFileTree(),
                t.getDestinationDirectory(), allClassesDirs, sourceDirs, t));
    }

    private static void configureCompileTask(FileTree sources, DirectoryProperty destinationDirectory,
            FileCollection allClassesDirs, List<SourceDir> sourceDirs, Task task) {
        if (!task.getEnabled()) {
            return;
        }
        if (sources.isEmpty()) {
            return;
        }
        final File destDir = destinationDirectory.getAsFile().get();
        if (!allClassesDirs.contains(destDir)) {
            return;
        }
        sources.visit(a -> {
            // we are looking for the root dirs containing sources
            if (a.getRelativePath().getSegments().length == 1) {
                final File srcDir = a.getFile().getParentFile();
                sourceDirs
                        .add(new DefaultSourceDir(srcDir.toPath(), destDir.toPath(), Map.of("compiler", task.getName())));
            }
        });
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

    private static boolean isFlagOn(byte walkingFlags, byte flag) {
        return (walkingFlags & flag) > 0;
    }

    private static byte clearFlag(byte flags, byte flag) {
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
                .setCoords(toArtifactCoords(a));
    }

    static ResolvedDependencyBuilder toDependency(ResolvedArtifact a, PathCollection paths, DefaultWorkspaceModule module,
            int... flags) {
        int allFlags = 0;
        for (int f : flags) {
            allFlags |= f;
        }
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(toArtifactCoords(a))
                .setResolvedPaths(paths)
                .setWorkspaceModule(module)
                .setFlags(allFlags);
    }

    private static ArtifactCoords toArtifactCoords(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        return new GACTV(split[0], split[1], a.getClassifier(), a.getType(), split.length > 2 ? split[2] : null);
    }

    private static ArtifactKey toAppDependenciesKey(String groupId, String artifactId, String classifier) {
        return new GACT(groupId, artifactId, classifier, ArtifactCoords.TYPE_JAR);
    }
}
