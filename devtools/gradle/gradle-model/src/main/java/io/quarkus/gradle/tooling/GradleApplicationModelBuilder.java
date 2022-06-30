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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

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

        final ApplicationDeploymentClasspathBuilder classpathBuilder = new ApplicationDeploymentClasspathBuilder(project, mode);
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

        final Map<ArtifactKey, ResolvedDependencyBuilder> appDependencies = new LinkedHashMap<>();
        collectDependencies(classpathConfig.getResolvedConfiguration(), workspaceDiscovery,
                project, appDependencies, modelBuilder, appArtifact.getWorkspaceModule().mutable());
        collectExtensionDependencies(project, deploymentConfig, appDependencies);

        for (ResolvedDependencyBuilder d : appDependencies.values()) {
            modelBuilder.addDependency(d.build());
        }
        return modelBuilder.build();
    }

    public static ResolvedDependency getProjectArtifact(Project project, boolean workspaceDiscovery) {
        final ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(project.getGroup().toString())
                .setArtifactId(project.getName())
                .setVersion(project.getVersion().toString());

        final JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention == null) {
            throw new GradleException("Failed to locate Java plugin extension in " + project.getPath());
        }
        final WorkspaceModule.Mutable mainModule = WorkspaceModule.builder()
                .setModuleId(new GAV(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getVersion()))
                .setModuleDir(project.getProjectDir().toPath())
                .setBuildDir(project.getBuildDir().toPath())
                .setBuildFile(project.getBuildFile().toPath());

        initProjectModule(project, mainModule, javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME),
                SourceSet.MAIN_SOURCE_SET_NAME, ArtifactSources.MAIN);
        if (workspaceDiscovery) {
            initProjectModule(project, mainModule, javaConvention.getSourceSets().findByName(SourceSet.TEST_SOURCE_SET_NAME),
                    SourceSet.TEST_SOURCE_SET_NAME, ArtifactSources.TEST);
        }

        final PathList.Builder paths = PathList.builder();
        collectDestinationDirs(mainModule.getMainSources().getSourceDirs(), paths);
        collectDestinationDirs(mainModule.getMainSources().getResourceDirs(), paths);

        return appArtifact.setWorkspaceModule(mainModule).setResolvedPaths(paths.build()).build();
    }

    private static void collectDestinationDirs(Collection<SourceDir> sources, final PathList.Builder paths) {
        for (SourceDir src : sources) {
            if (!Files.exists(src.getOutputDir())) {
                continue;
            }

            final Path path = src.getOutputDir();
            if (paths.contains(path)) {
                continue;
            }
            paths.add(path);
        }
    }

    private void collectExtensionDependencies(Project project, Configuration deploymentConfiguration,
            Map<ArtifactKey, ResolvedDependencyBuilder> appDependencies) {
        final ResolvedConfiguration rc = deploymentConfiguration.getResolvedConfiguration();
        for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
            if (a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
                final Project projectDep = project.getRootProject().findProject(
                        ((ProjectComponentIdentifier) a.getId().getComponentIdentifier()).getProjectPath());
                final JavaPluginConvention javaExtension = projectDep == null ? null
                        : projectDep.getConvention().findPlugin(JavaPluginConvention.class);
                SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                final ResolvedDependencyBuilder dep = appDependencies.computeIfAbsent(
                        toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier()),
                        k -> toDependency(a, mainSourceSet));
                dep.setDeploymentCp();
                dep.clearFlag(DependencyFlags.RELOADABLE);
            } else if (isDependency(a)) {
                final ResolvedDependencyBuilder dep = appDependencies.computeIfAbsent(
                        toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier()),
                        k -> toDependency(a));
                dep.setDeploymentCp();
                dep.clearFlag(DependencyFlags.RELOADABLE);
            }
        }
    }

    private void collectDependencies(ResolvedConfiguration configuration,
            boolean workspaceDiscovery, Project project, Map<ArtifactKey, ResolvedDependencyBuilder> appDependencies,
            ApplicationModelBuilder modelBuilder, WorkspaceModule.Mutable wsModule) {

        final Set<ResolvedArtifact> resolvedArtifacts = configuration.getResolvedArtifacts();
        // if the number of artifacts is less than the number of files then probably
        // the project includes direct file dependencies
        final Set<File> artifactFiles = resolvedArtifacts.size() < configuration.getFiles().size()
                ? new HashSet<>(resolvedArtifacts.size())
                : null;

        configuration.getFirstLevelModuleDependencies()
                .forEach(d -> {
                    collectDependencies(d, workspaceDiscovery, project, appDependencies, artifactFiles, new HashSet<>(),
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
                        .setRuntimeCp();
                processQuarkusDependency(artifactBuilder, modelBuilder);
                appDependencies.put(artifactBuilder.getKey(), artifactBuilder);
            }
        }
    }

    private void collectDependencies(org.gradle.api.artifacts.ResolvedDependency resolvedDep, boolean workspaceDiscovery,
            Project project,
            Map<ArtifactKey, ResolvedDependencyBuilder> appDependencies, Set<File> artifactFiles,
            Set<ArtifactKey> processedModules, ApplicationModelBuilder modelBuilder, WorkspaceModule.Mutable parentModule,
            byte flags) {
        WorkspaceModule.Mutable projectModule = null;
        for (ResolvedArtifact a : resolvedDep.getModuleArtifacts()) {
            final ArtifactKey artifactKey = toAppDependenciesKey(a.getModuleVersion().getId().getGroup(), a.getName(),
                    a.getClassifier());
            if (!isDependency(a) || appDependencies.containsKey(artifactKey)) {
                continue;
            }
            final ArtifactCoords depCoords = toArtifactCoords(a);
            final ResolvedDependencyBuilder depBuilder = ResolvedDependencyBuilder.newInstance()
                    .setCoords(depCoords)
                    .setRuntimeCp();
            if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                depBuilder.setDirect(true);
                flags = clearFlag(flags, COLLECT_DIRECT_DEPS);
            }
            if (parentModule != null) {
                parentModule.addDependency(new ArtifactDependency(depCoords));
            }

            PathCollection paths = null;
            if (workspaceDiscovery && a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {

                final Project projectDep = project.getRootProject().findProject(
                        ((ProjectComponentIdentifier) a.getId().getComponentIdentifier()).getProjectPath());
                final JavaPluginConvention javaExtension = projectDep == null ? null
                        : projectDep.getConvention().findPlugin(JavaPluginConvention.class);

                final String classifier = a.getClassifier();
                if (classifier == null || classifier.isEmpty()) {
                    final IncludedBuild includedBuild = ToolingUtils.includedBuild(project.getRootProject(), a.getName());
                    if (includedBuild != null) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        addSubstitutedProject(pathBuilder, includedBuild.getProjectDir());
                        paths = pathBuilder.build();
                    } else if (javaExtension != null) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                javaExtension, pathBuilder, SourceSet.MAIN_SOURCE_SET_NAME, false);
                        paths = pathBuilder.build();
                    }
                } else if (javaExtension != null) {
                    if ("test".equals(classifier)) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                javaExtension, pathBuilder, SourceSet.TEST_SOURCE_SET_NAME, true);
                        paths = pathBuilder.build();
                    } else if ("test-fixtures".equals(classifier)) {
                        final PathList.Builder pathBuilder = PathList.builder();
                        projectModule = initProjectModuleAndBuildPaths(projectDep, a, modelBuilder, depBuilder,
                                javaExtension, pathBuilder, "testFixtures", true);
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
            appDependencies.put(depBuilder.getKey(), depBuilder);

            if (artifactFiles != null) {
                artifactFiles.add(a.getFile());
            }
        }

        processedModules.add(new GACT(resolvedDep.getModuleGroup(), resolvedDep.getModuleName()));
        for (org.gradle.api.artifacts.ResolvedDependency child : resolvedDep.getChildren()) {
            if (!processedModules.contains(new GACT(child.getModuleGroup(), child.getModuleName()))) {
                collectDependencies(child, workspaceDiscovery, project, appDependencies, artifactFiles, processedModules,
                        modelBuilder,
                        projectModule, flags);
            }
        }
    }

    private static String toNonNullClassifier(String resolvedClassifier) {
        return resolvedClassifier == null ? "" : resolvedClassifier;
    }

    private WorkspaceModule.Mutable initProjectModuleAndBuildPaths(final Project project,
            ResolvedArtifact resolvedArtifact, ApplicationModelBuilder appModel, final ResolvedDependencyBuilder appDep,
            final JavaPluginConvention javaExt, PathList.Builder buildPaths, String sourceName, boolean test) {

        appDep.setWorkspaceModule().setReloadable();

        final WorkspaceModule.Mutable projectModule = appModel.getOrCreateProjectModule(
                new GAV(resolvedArtifact.getModuleVersion().getId().getGroup(), resolvedArtifact.getName(),
                        resolvedArtifact.getModuleVersion().getId().getVersion()),
                project.getProjectDir(),
                project.getBuildDir())
                .setBuildFile(project.getBuildFile().toPath());

        final String classifier = toNonNullClassifier(resolvedArtifact.getClassifier());
        initProjectModule(project, projectModule, javaExt.getSourceSets().findByName(sourceName), sourceName, classifier);

        collectDestinationDirs(projectModule.getSources(classifier).getSourceDirs(), buildPaths);
        collectDestinationDirs(projectModule.getSources(classifier).getResourceDirs(), buildPaths);

        appModel.addReloadableWorkspaceModule(
                new GACT(resolvedArtifact.getModuleVersion().getId().getGroup(), resolvedArtifact.getName(), classifier,
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
            String sourceName, String classifier) {

        if (sourceSet == null) {
            return;
        }

        final FileCollection allClassesDirs = sourceSet.getOutput().getClassesDirs();
        // some plugins do not add source directories to source sets and they may be missing from sourceSet.getAllJava()
        // see https://github.com/quarkusio/quarkus/issues/20755

        final List<SourceDir> sourceDirs = new ArrayList<>(1);
        final List<SourceDir> resourceDirs = new ArrayList<>(1);
        project.getTasks().withType(AbstractCompile.class, t -> {
            if (!t.getEnabled()) {
                return;
            }
            final FileTree source = t.getSource();
            if (source.isEmpty()) {
                return;
            }
            final File destDir = t.getDestinationDirectory().getAsFile().get();
            if (!allClassesDirs.contains(destDir)) {
                return;
            }
            final List<File> srcDirs = new ArrayList<>(1);
            source.visit(a -> {
                // we are looking for the root dirs containing sources
                if (a.getRelativePath().getSegments().length == 1) {
                    final File srcDir = a.getFile().getParentFile();
                    if (srcDirs.add(srcDir)) {
                        DefaultSourceDir sources = new DefaultSourceDir(srcDir.toPath(), destDir.toPath(),
                                Collections.singletonMap("compiler", t.getName()));
                        sourceDirs.add(sources);
                    }
                }
            });
        });

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
            final List<File> srcDirs = new ArrayList<>(1);
            source.getAsFileTree().visit(a -> {
                // we are looking for the root dirs containing sources
                if (a.getRelativePath().getSegments().length == 1) {
                    final File srcDir = a.getFile().getParentFile();
                    if (srcDirs.add(srcDir)) {
                        resourceDirs.add(new DefaultSourceDir(srcDir.toPath(), destDir));
                    }
                }
            });
        });
        // there could be a task generating resources
        if (resourcesOutputDir.exists() && resourceDirs.isEmpty()) {
            sourceSet.getResources().getSrcDirs()
                    .forEach(srcDir -> resourceDirs.add(new DefaultSourceDir(srcDir.toPath(), resourcesOutputDir.toPath())));
        }
        module.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resourceDirs));
    }

    private void addSubstitutedProject(PathList.Builder paths, File projectFile) {
        File mainResourceDirectory = new File(projectFile, MAIN_RESOURCES_OUTPUT);
        if (mainResourceDirectory.exists()) {
            paths.add(mainResourceDirectory.toPath());
        }
        File classesOutput = new File(projectFile, CLASSES_OUTPUT);
        File[] languageDirectories = classesOutput.listFiles();
        if (languageDirectories == null) {
            throw new GradleException(
                    "The project does not contain a class output directory. " + classesOutput.getPath() + " must exist.");
        }
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
