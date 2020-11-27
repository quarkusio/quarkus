package io.quarkus.gradle.builder;

import static io.quarkus.bootstrap.resolver.model.impl.ArtifactCoordsImpl.TYPE_JAR;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.attributes.Category;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.model.ArtifactCoords;
import io.quarkus.bootstrap.resolver.model.Dependency;
import io.quarkus.bootstrap.resolver.model.ModelParameter;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;
import io.quarkus.bootstrap.resolver.model.impl.ArtifactCoordsImpl;
import io.quarkus.bootstrap.resolver.model.impl.DependencyImpl;
import io.quarkus.bootstrap.resolver.model.impl.ModelParameterImpl;
import io.quarkus.bootstrap.resolver.model.impl.QuarkusModelImpl;
import io.quarkus.bootstrap.resolver.model.impl.SourceSetImpl;
import io.quarkus.bootstrap.resolver.model.impl.WorkspaceImpl;
import io.quarkus.bootstrap.resolver.model.impl.WorkspaceModuleImpl;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;

public class QuarkusModelBuilder implements ParameterizedToolingModelBuilder<ModelParameter> {

    private static Configuration classpathConfig(Project project, LaunchMode mode) {
        if (LaunchMode.TEST.equals(mode)) {
            return project.getConfigurations().getByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        }
        if (LaunchMode.DEVELOPMENT.equals(mode)) {
            return project.getConfigurations().create("quarkusDevMode").extendsFrom(
                    project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
                    project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
        }
        return project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(QuarkusModel.class.getName());
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
        LaunchMode mode = LaunchMode.valueOf(parameter.getMode());

        final List<org.gradle.api.artifacts.Dependency> deploymentDeps = getEnforcedPlatforms(project);

        final Map<String, String> platformProperties = resolvePlatformProperties(project, deploymentDeps);

        final Map<ArtifactCoords, Dependency> appDependencies = new LinkedHashMap<>();
        final Set<ArtifactCoords> visitedDeps = new HashSet<>();

        final ResolvedConfiguration configuration = classpathConfig(project, mode).getResolvedConfiguration();
        collectDependencies(configuration, mode, project, appDependencies);
        collectFirstMetDeploymentDeps(configuration.getFirstLevelModuleDependencies(), appDependencies,
                deploymentDeps, visitedDeps);

        final List<Dependency> extensionDependencies = collectExtensionDependencies(project, deploymentDeps);

        ArtifactCoords appArtifactCoords = new ArtifactCoordsImpl(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());

        return new QuarkusModelImpl(new WorkspaceImpl(appArtifactCoords, getWorkspace(project.getRootProject(), mode)),
                new LinkedList<>(appDependencies.values()),
                extensionDependencies,
                deploymentDeps.stream().map(QuarkusModelBuilder::toEnforcedPlatformDependency)
                        .filter(Objects::nonNull).collect(Collectors.toList()),
                platformProperties);
    }

    private Map<String, String> resolvePlatformProperties(Project project,
            List<org.gradle.api.artifacts.Dependency> deploymentDeps) {
        final Configuration boms = project.getConfigurations()
                .detachedConfiguration(deploymentDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));
        final Map<String, String> platformProps = new HashMap<>();
        final Set<AppArtifactKey> descriptorKeys = new HashSet<>(4);
        final Set<AppArtifactKey> propertyKeys = new HashSet<>(2);
        boms.getResolutionStrategy().eachDependency(d -> {
            final String group = d.getTarget().getGroup();
            final String name = d.getTarget().getName();
            if (name.endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                descriptorKeys.add(new AppArtifactKey(group,
                        name.substring(0, name.length() - BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()),
                        d.getTarget().getVersion()));
            } else if (name.endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                final DefaultDependencyArtifact dep = new DefaultDependencyArtifact();
                dep.setExtension("properties");
                dep.setType("properties");
                dep.setName(name);

                final DefaultExternalModuleDependency gradleDep = new DefaultExternalModuleDependency(
                        group, name, d.getTarget().getVersion(), null);
                gradleDep.addArtifact(dep);

                for (ResolvedArtifact a : project.getConfigurations().detachedConfiguration(gradleDep)
                        .getResolvedConfiguration().getResolvedArtifacts()) {
                    if (a.getName().equals(name)) {
                        final Properties props = new Properties();
                        try (InputStream is = new FileInputStream(a.getFile())) {
                            props.load(is);
                        } catch (IOException e) {
                            throw new GradleException("Failed to read properties from " + a.getFile(), e);
                        }
                        for (Map.Entry<?, ?> prop : props.entrySet()) {
                            final String propName = String.valueOf(prop.getKey());
                            if (propName.startsWith(BootstrapConstants.PLATFORM_PROPERTY_PREFIX)) {
                                platformProps.put(propName, String.valueOf(prop.getValue()));
                            }
                        }
                        break;
                    }
                }
                propertyKeys.add(new AppArtifactKey(group,
                        name.substring(0, name.length() - BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX.length()),
                        d.getTarget().getVersion()));
            }

        });
        boms.getResolvedConfiguration();
        if (!descriptorKeys.containsAll(propertyKeys)) {
            final StringBuilder buf = new StringBuilder();
            buf.append(
                    "The Quarkus platform properties applied to the project are missing the corresponding Quarkus platform BOM imports:");
            final int l = buf.length();
            for (AppArtifactKey key : propertyKeys) {
                if (!descriptorKeys.contains(key)) {
                    if (l - buf.length() < 0) {
                        buf.append(',');
                    }
                    buf.append(' ').append(key);
                }
            }
            throw new GradleException(buf.toString());
        }
        return platformProps;
    }

    public Set<WorkspaceModule> getWorkspace(Project project, LaunchMode mode) {
        Set<WorkspaceModule> modules = new HashSet<>();
        for (Project subproject : project.getAllprojects()) {
            final Convention convention = subproject.getConvention();
            JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
            if (javaConvention == null || !javaConvention.getSourceSets().getNames().contains(SourceSet.MAIN_SOURCE_SET_NAME)) {
                continue;
            }
            modules.add(getWorkspaceModule(subproject, mode));
        }
        return modules;
    }

    private WorkspaceModule getWorkspaceModule(Project project, LaunchMode mode) {
        ArtifactCoords appArtifactCoords = new ArtifactCoordsImpl(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());
        final SourceSet mainSourceSet = QuarkusGradleUtils.getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSetImpl modelSourceSet = convert(mainSourceSet);
        return new WorkspaceModuleImpl(appArtifactCoords, project.getProjectDir().getAbsoluteFile(),
                project.getBuildDir().getAbsoluteFile(), getSourceSourceSet(mainSourceSet), modelSourceSet);
    }

    private List<org.gradle.api.artifacts.Dependency> getEnforcedPlatforms(Project project) {
        final List<org.gradle.api.artifacts.Dependency> directExtension = new ArrayList<>();
        // collect enforced platforms
        final Configuration impl = project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);

        for (org.gradle.api.artifacts.Dependency d : impl.getAllDependencies()) {
            if (!(d instanceof ModuleDependency)) {
                continue;
            }
            final ModuleDependency module = (ModuleDependency) d;
            final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
            if (category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                    || Category.REGULAR_PLATFORM.equals(category.getName()))) {
                directExtension.add(d);
            }
        }
        return directExtension;
    }

    private void collectFirstMetDeploymentDeps(Set<ResolvedDependency> dependencies,
            Map<ArtifactCoords, Dependency> appDependencies, List<org.gradle.api.artifacts.Dependency> extensionDeps,
            Set<ArtifactCoords> visited) {
        for (ResolvedDependency d : dependencies) {
            boolean addChildExtension = true;
            boolean wasDependencyVisited = false;
            for (ResolvedArtifact artifact : d.getModuleArtifacts()) {
                ModuleVersionIdentifier moduleIdentifier = artifact.getModuleVersion().getId();
                ArtifactCoords key = toAppDependenciesKey(moduleIdentifier.getGroup(), moduleIdentifier.getName(),
                        artifact.getClassifier());
                if (!visited.add(key)) {
                    continue;
                }

                Dependency appDep = appDependencies.get(key);
                if (appDep == null) {
                    continue;
                }

                wasDependencyVisited = true;
                final org.gradle.api.artifacts.Dependency deploymentArtifact = getDeploymentArtifact(appDep);
                if (deploymentArtifact != null) {
                    extensionDeps.add(deploymentArtifact);
                    addChildExtension = false;
                }
            }
            final Set<ResolvedDependency> resolvedChildren = d.getChildren();
            if (wasDependencyVisited && addChildExtension && !resolvedChildren.isEmpty()) {
                collectFirstMetDeploymentDeps(resolvedChildren, appDependencies, extensionDeps, visited);
            }
        }
    }

    private org.gradle.api.artifacts.Dependency getDeploymentArtifact(Dependency dependency) {
        for (File file : dependency.getPaths()) {
            if (!file.exists()) {
                continue;
            }
            Properties depsProperties;
            if (file.isDirectory()) {
                Path quarkusDescr = file.toPath()
                        .resolve(BootstrapConstants.META_INF)
                        .resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
                if (!Files.exists(quarkusDescr)) {
                    continue;
                }
                depsProperties = QuarkusModelHelper.resolveDescriptor(quarkusDescr);
            } else {
                try (FileSystem artifactFs = FileSystems.newFileSystem(file.toPath(), getClass().getClassLoader())) {
                    Path quarkusDescr = artifactFs.getPath(BootstrapConstants.META_INF)
                            .resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
                    if (!Files.exists(quarkusDescr)) {
                        continue;
                    }
                    depsProperties = QuarkusModelHelper.resolveDescriptor(quarkusDescr);
                } catch (IOException e) {
                    throw new GradleException("Failed to process " + file, e);
                }
            }
            String value = depsProperties.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            String[] split = value.split(":");
            return new DefaultExternalModuleDependency(split[0], split[1], split[2], null);
        }
        return null;
    }

    private List<Dependency> collectExtensionDependencies(Project project,
            Collection<org.gradle.api.artifacts.Dependency> extensions) {
        final List<Dependency> platformDependencies = new LinkedList<>();

        final Configuration deploymentConfig = project.getConfigurations()
                .detachedConfiguration(extensions.toArray(new org.gradle.api.artifacts.Dependency[0]));
        final ResolvedConfiguration rc = deploymentConfig.getResolvedConfiguration();
        for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
            if (!isDependency(a)) {
                continue;
            }

            final Dependency dependency = toDependency(a);
            platformDependencies.add(dependency);
        }

        return platformDependencies;
    }

    private void collectDependencies(ResolvedConfiguration configuration,
            LaunchMode mode, Project project, Map<ArtifactCoords, Dependency> appDependencies) {
        final Set<ResolvedArtifact> artifacts = configuration.getResolvedArtifacts();
        Set<File> artifactFiles = null;
        // if the number of artifacts is less than the number of files then probably
        // the project includes direct file dependencies
        if (artifacts.size() < configuration.getFiles().size()) {
            artifactFiles = new HashSet<>(artifacts.size());
        }
        for (ResolvedArtifact a : artifacts) {
            if (!isDependency(a)) {
                continue;
            }
            final DependencyImpl dep = initDependency(a);
            if (LaunchMode.DEVELOPMENT.equals(mode) &&
                    a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
                Project projectDep = project.getRootProject()
                        .findProject(((ProjectComponentIdentifier) a.getId().getComponentIdentifier()).getProjectPath());
                addDevModePaths(dep, a, projectDep);
            } else {
                dep.addPath(a.getFile());
            }
            appDependencies.put(toAppDependenciesKey(dep.getGroupId(), dep.getName(), dep.getClassifier()), dep);
            if (artifactFiles != null) {
                artifactFiles.add(a.getFile());
            }
        }

        if (artifactFiles != null) {
            // detect FS paths that aren't provided by the resolved artifacts
            for (File f : configuration.getFiles()) {
                if (artifactFiles.contains(f)) {
                    continue;
                }
                // here we are trying to represent a direct FS path dependency
                // as an artifact dependency
                // SHA1 hash is used to avoid long file names in the lib dir
                final String parentPath = f.getParent();
                final String group = HashUtil.sha1(parentPath == null ? f.getName() : parentPath);
                String name = f.getName();
                String type = "jar";
                if (!f.isDirectory()) {
                    final int dot = f.getName().lastIndexOf('.');
                    if (dot > 0) {
                        name = f.getName().substring(0, dot);
                        type = f.getName().substring(dot + 1);
                    }
                }
                // hash could be a better way to represent the version
                final String version = String.valueOf(f.lastModified());
                final ArtifactCoords key = toAppDependenciesKey(group, name, "");
                final DependencyImpl dep = new DependencyImpl(name, group, version, "compile", type, null);
                dep.addPath(f);
                appDependencies.put(key, dep);
            }
        }
    }

    private void addDevModePaths(final DependencyImpl dep, ResolvedArtifact a, Project project) {
        final JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention == null) {
            dep.addPath(a.getFile());
            return;
        }
        final SourceSet mainSourceSet = javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (mainSourceSet == null) {
            dep.addPath(a.getFile());
            return;
        }

        final File classesDir = new File(QuarkusGradleUtils.getClassesDir(mainSourceSet, project.getBuildDir(), false));
        if (classesDir.exists()) {
            dep.addPath(classesDir);
        }
        for (File resourcesDir : mainSourceSet.getResources().getSourceDirectories()) {
            if (resourcesDir.exists()) {
                dep.addPath(resourcesDir);
            }
        }
        final Task resourcesTask = project.getTasks().findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
        for (File outputDir : resourcesTask.getOutputs().getFiles()) {
            if (outputDir.exists()) {
                dep.addPath(outputDir);
            }
        }
    }

    private SourceSetImpl convert(SourceSet sourceSet) {
        if (sourceSet.getOutput().getResourcesDir().exists()) {
            return new SourceSetImpl(
                    sourceSet.getOutput().getClassesDirs().getFiles(),
                    sourceSet.getOutput().getResourcesDir());
        }
        return new SourceSetImpl(
                sourceSet.getOutput().getClassesDirs().getFiles());
    }

    private io.quarkus.bootstrap.resolver.model.SourceSet getSourceSourceSet(SourceSet sourceSet) {
        return new SourceSetImpl(sourceSet.getAllJava().getSrcDirs(),
                sourceSet.getResources().getSourceDirectories().getSingleFile());
    }

    private static boolean isDependency(ResolvedArtifact a) {
        return BootstrapConstants.JAR.equalsIgnoreCase(a.getExtension()) || "exe".equalsIgnoreCase(a.getExtension()) ||
                a.getFile().isDirectory();
    }

    /**
     * Creates an instance of Dependency and associates it with the ResolvedArtifact's path
     */
    static Dependency toDependency(ResolvedArtifact a) {
        final DependencyImpl dependency = initDependency(a);
        dependency.addPath(a.getFile());
        return dependency;
    }

    static Dependency toEnforcedPlatformDependency(org.gradle.api.artifacts.Dependency dependency) {
        if (dependency instanceof ExternalModuleDependency) {
            ExternalModuleDependency emd = (ExternalModuleDependency) dependency;
            Category category = emd.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
            if (category != null && Category.ENFORCED_PLATFORM.equals(category.getName())) {
                return new DependencyImpl(emd.getName(), emd.getGroup(), emd.getVersion(),
                        "compile", "pom", null);
            }
        }
        return null;
    }

    /**
     * Creates an instance of DependencyImpl but does not associates it with a path
     */
    private static DependencyImpl initDependency(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        return new DependencyImpl(split[1], split[0], split.length > 2 ? split[2] : null,
                "compile", a.getType(), a.getClassifier());
    }

    private static ArtifactCoords toAppDependenciesKey(String groupId, String artifactId, String classifier) {
        // Default classifier is empty string and not null value, lets keep it that way
        classifier = classifier == null ? "" : classifier;
        return new ArtifactCoordsImpl(groupId, artifactId, classifier, "", TYPE_JAR);
    }
}
