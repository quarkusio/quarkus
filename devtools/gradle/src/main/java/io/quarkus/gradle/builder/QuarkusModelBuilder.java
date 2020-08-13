package io.quarkus.gradle.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.attributes.Category;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaTestFixturesPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.component.external.model.TestFixturesSupport;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

import io.quarkus.bootstrap.BootstrapConstants;
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
        LaunchMode mode = LaunchMode.valueOf(((ModelParameter) parameter).getMode());

        final Set<org.gradle.api.artifacts.Dependency> deploymentDeps = getEnforcedPlatforms(project);
        final Map<ArtifactCoords, Dependency> appDependencies = new HashMap<>();
        final Set<ArtifactCoords> visitedDeps = new HashSet<>();

        final ResolvedConfiguration configuration = classpathConfig(project, mode).getResolvedConfiguration();
        collectDependencies(configuration, mode, project, appDependencies);
        collectFirstMetDeploymentDeps(configuration.getFirstLevelModuleDependencies(), appDependencies,
                deploymentDeps, visitedDeps);

        final Set<Dependency> extensionDependencies = collectExtensionDependencies(project, deploymentDeps);

        ArtifactCoords appArtifactCoords = new ArtifactCoordsImpl(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());

        return new QuarkusModelImpl(new WorkspaceImpl(appArtifactCoords, getWorkspace(project.getRootProject(), mode)),
                new HashSet<>(appDependencies.values()),
                extensionDependencies);
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

        // If the project exposes test fixtures, we must add them as source directory
        if (mode.equals(LaunchMode.TEST) && project.getPlugins().hasPlugin(JavaTestFixturesPlugin.class)) {
            final SourceSet fixtureSourceSet = QuarkusGradleUtils.getSourceSet(project,
                    TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME);
            modelSourceSet.addSourceDirectories(fixtureSourceSet.getOutput().getClassesDirs().getFiles());
        }

        return new WorkspaceModuleImpl(appArtifactCoords, project.getProjectDir().getAbsoluteFile(),
                project.getBuildDir().getAbsoluteFile(), getSourceSourceSet(mainSourceSet), modelSourceSet);
    }

    private Set<org.gradle.api.artifacts.Dependency> getEnforcedPlatforms(Project project) {
        final Set<org.gradle.api.artifacts.Dependency> directExtension = new HashSet<>();
        // collect enforced platforms
        final Configuration impl = project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        for (org.gradle.api.artifacts.Dependency d : impl.getAllDependencies()) {
            if (!(d instanceof ModuleDependency)) {
                continue;
            }
            final ModuleDependency module = (ModuleDependency) d;
            final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
            if (category != null && Category.ENFORCED_PLATFORM.equals(category.getName())) {
                directExtension.add(d);
            }
        }
        return directExtension;
    }

    private void collectFirstMetDeploymentDeps(Set<ResolvedDependency> dependencies,
            Map<ArtifactCoords, Dependency> appDependencies, Set<org.gradle.api.artifacts.Dependency> extensionDeps,
            Set<ArtifactCoords> visited) {
        for (ResolvedDependency d : dependencies) {
            ArtifactCoords key = new ArtifactCoordsImpl(d.getModuleGroup(), d.getModuleName(), "");
            if (!visited.add(key)) {
                continue;
            }

            Dependency appDep = appDependencies.get(key);
            if (appDep == null) {
                continue;
            }
            final org.gradle.api.artifacts.Dependency deploymentArtifact = getDeploymentArtifact(appDep);

            boolean addChildExtension = true;
            if (deploymentArtifact != null && addChildExtension) {
                extensionDeps.add(deploymentArtifact);
                addChildExtension = false;
            }

            final Set<ResolvedDependency> resolvedChildren = d.getChildren();
            if (addChildExtension && !resolvedChildren.isEmpty()) {
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

    private Set<Dependency> collectExtensionDependencies(Project project,
            Collection<org.gradle.api.artifacts.Dependency> extensions) {
        final Set<Dependency> platformDependencies = new HashSet<>();

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
        for (ResolvedArtifact a : configuration.getResolvedArtifacts()) {
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
            appDependencies.put((ArtifactCoords) new ArtifactCoordsImpl(dep.getGroupId(), dep.getName(), ""), dep);
        }
    }

    private void addDevModePaths(final DependencyImpl dep, ResolvedArtifact a, Project project) {
        final JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention != null) {
            SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            final File classesDir = new File(QuarkusGradleUtils.getClassesDir(mainSourceSet, project.getBuildDir(), false));
            if (classesDir.exists()) {
                dep.addPath(classesDir);
            }
            for (File resourcesDir : mainSourceSet.getResources().getSourceDirectories()) {
                if (resourcesDir.exists()) {
                    dep.addPath(resourcesDir);
                }
            }
            for (File outputDir : project.getTasks().findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
                    .getOutputs().getFiles()) {
                if (outputDir.exists()) {
                    dep.addPath(outputDir);
                }
            }
        } else {
            dep.addPath(a.getFile());
        }
    }

    private SourceSetImpl convert(SourceSet sourceSet) {
        return new SourceSetImpl(
                sourceSet.getOutput().getClassesDirs().getFiles(),
                sourceSet.getOutput().getResourcesDir());
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

    /**
     * Creates an instance of DependencyImpl but does not associates it with a path
     */
    private static DependencyImpl initDependency(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        return new DependencyImpl(split[1], split[0], split.length > 2 ? split[2] : null,
                "compile", a.getType(), a.getClassifier());
    }
}
