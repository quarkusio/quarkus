package io.quarkus.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.runtime.LaunchMode;

public class AppModelGradleResolver implements AppModelResolver {

    private AppModel appModel;

    private final Project project;
    private final LaunchMode launchMode;
    private final Map<AppArtifactKey, Task> projectDepJarTasks;

    public AppModelGradleResolver(Project project, LaunchMode mode, Map<AppArtifactKey, Task> projectDepJarTasks) {
        this.project = project;
        this.launchMode = mode;
        this.projectDepJarTasks = projectDepJarTasks;
    }

    @Override
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestVersionFromRange(AppArtifact appArtifact, String range) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String fromVersion, boolean fromVersionIncluded, String upToVersion,
            boolean upToVersionIncluded)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void relink(AppArtifact appArtifact, Path localPath) throws AppModelResolverException {

    }

    @Override
    public Path resolve(AppArtifact appArtifact) throws AppModelResolverException {
        if (!appArtifact.isResolved()) {

            final DefaultDependencyArtifact dep = new DefaultDependencyArtifact();
            dep.setExtension(appArtifact.getType());
            dep.setType(appArtifact.getType());
            dep.setName(appArtifact.getArtifactId());

            final DefaultExternalModuleDependency gradleDep = new DefaultExternalModuleDependency(appArtifact.getGroupId(),
                    appArtifact.getArtifactId(), appArtifact.getVersion(), null);
            gradleDep.addArtifact(dep);

            final Configuration detachedConfig = project.getConfigurations().detachedConfiguration(gradleDep);

            final ResolvedConfiguration rc = detachedConfig.getResolvedConfiguration();
            Set<ResolvedArtifact> resolvedArtifacts = rc.getResolvedArtifacts();
            for (ResolvedArtifact a : resolvedArtifacts) {
                if (appArtifact.getArtifactId().equals(a.getName())
                        && appArtifact.getType().equals(a.getType())
                        && appArtifact.getGroupId().equals(a.getModuleVersion().getId().getGroup())) {
                    appArtifact.setPath(a.getFile().toPath());
                }
            }

            if (!appArtifact.isResolved()) {
                throw new AppModelResolverException("Failed to resolve " + appArtifact);
            }
        }
        return appArtifact.getPath();
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact appArtifact, List<AppDependency> directDeps) {
        return Collections.emptyList();
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact) throws AppModelResolverException {
        AppModel.Builder appBuilder = new AppModel.Builder();
        if (appModel != null && appModel.getAppArtifact().equals(appArtifact)) {
            return appModel;
        }
        final List<Dependency> directExtensionDeps = new ArrayList<>();
        final List<AppDependency> userDeps = new ArrayList<>();
        Map<AppArtifactKey, AppDependency> versionMap = new HashMap<>();
        Map<ModuleIdentifier, ModuleVersionIdentifier> userModules = new HashMap<>();

        collectDependencies(appBuilder, directExtensionDeps, userDeps, versionMap, userModules);

        final List<AppDependency> deploymentDeps = new ArrayList<>();
        final List<AppDependency> fullDeploymentDeps = new ArrayList<>();
        if (!directExtensionDeps.isEmpty()) {
            final Configuration deploymentConfig = project.getConfigurations()
                    .detachedConfiguration(directExtensionDeps.toArray(new Dependency[0]));
            final ResolvedConfiguration rc = deploymentConfig.getResolvedConfiguration();
            for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
                final ModuleVersionIdentifier userVersion = userModules.get(getModuleId(a));
                if (userVersion != null || !isDependency(a)) {
                    continue;
                }
                final AppDependency dependency = toAppDependency(a);
                fullDeploymentDeps.add(dependency);
                if (!userDeps.contains(dependency)) {
                    AppDependency deploymentDep = alignVersion(dependency, versionMap);
                    deploymentDeps.add(deploymentDep);
                }
            }
        }
        fullDeploymentDeps.addAll(userDeps);

        if (!appArtifact.isResolved()) {
            final Jar jarTask = (Jar) project.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
            if (jarTask == null) {
                throw new AppModelResolverException("Failed to locate task 'jar' in the project.");
            }
            final Provider<RegularFile> jarProvider = jarTask.getArchiveFile();
            Path classesDir = null;
            if (jarProvider.isPresent()) {
                final File f = jarProvider.get().getAsFile();
                if (f.exists()) {
                    classesDir = f.toPath();
                }
            }
            if (classesDir == null) {
                final Convention convention = project.getConvention();
                JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
                if (javaConvention != null) {
                    final SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    final String classesPath = QuarkusGradleUtils.getClassesDir(mainSourceSet, jarTask);
                    if (classesPath != null) {
                        classesDir = Paths.get(classesPath);
                    }
                }
            }
            if (classesDir == null) {
                throw new AppModelResolverException("Failed to locate classes directory for " + appArtifact);
            }
            appArtifact.setPath(classesDir);
        }
        appBuilder.addRuntimeDeps(userDeps)
                .addFullDeploymentDeps(fullDeploymentDeps)
                .addDeploymentDeps(deploymentDeps)
                .setAppArtifact(appArtifact);
        return this.appModel = appBuilder.build();
    }

    private void collectDependencies(AppModel.Builder appBuilder, final List<Dependency> directExtensionDeps,
            final List<AppDependency> userDeps, Map<AppArtifactKey, AppDependency> versionMap,
            Map<ModuleIdentifier, ModuleVersionIdentifier> userModules) {
        collectDependencies(project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME),
                appBuilder, directExtensionDeps, userDeps,
                versionMap, userModules);
        if (launchMode == LaunchMode.TEST) {
            collectDependencies(project.getConfigurations().getByName(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME),
                    appBuilder, directExtensionDeps, userDeps,
                    versionMap, userModules);
        }
    }

    private void collectDependencies(Configuration config, AppModel.Builder appBuilder,
            final List<Dependency> directExtensionDeps,
            final List<AppDependency> userDeps, Map<AppArtifactKey, AppDependency> versionMap,
            Map<ModuleIdentifier, ModuleVersionIdentifier> userModules) {

        final ResolvedConfiguration resolvedConfig = config.getResolvedConfiguration();
        for (ResolvedArtifact a : resolvedConfig.getResolvedArtifacts()) {
            if (!isDependency(a)) {
                continue;
            }
            userModules.put(getModuleId(a), a.getModuleVersion().getId());

            final AppDependency dependency = toAppDependency(a);
            final AppArtifactKey artifactGa = new AppArtifactKey(dependency.getArtifact().getGroupId(),
                    dependency.getArtifact().getArtifactId());

            // If we are building a JAR then we should be including dependencies as JARs, not directories of classes
            final Task jarTask = !LaunchMode.NORMAL.equals(launchMode) ? null : projectDepJarTasks.get(artifactGa);
            if (jarTask != null) {
                final Iterator<File> i = jarTask.getOutputs().getFiles().iterator();
                if (!i.hasNext()) {
                    project.getLogger()
                            .warn("The jar task of " + artifactGa + " contains no output, falling back to classes dir "
                                    + a.getFile());
                } else {
                    final Path jarPath = i.next().toPath();
                    if (i.hasNext()) {
                        project.getLogger().warn("The jar task of " + artifactGa
                                + " produced more than one JAR which is not currently supported, falling back to classes dir "
                                + a.getFile());
                    } else {
                        dependency.getArtifact().setPath(jarPath);
                    }
                }
            }

            userDeps.add(dependency);
            versionMap.put(artifactGa, dependency);
        }

        collectExtensionDeps(resolvedConfig.getFirstLevelModuleDependencies(), versionMap, appBuilder, directExtensionDeps,
                true, new HashSet<>());
    }

    private void collectExtensionDeps(Set<ResolvedDependency> resolvedDeps,
            Map<AppArtifactKey, AppDependency> versionMap,
            AppModel.Builder appBuilder,
            List<Dependency> firstLevelExtensions,
            boolean firstLevelExt,
            Set<AppArtifactKey> visited) {
        for (ResolvedDependency dep : resolvedDeps) {
            final AppArtifactKey key = new AppArtifactKey(dep.getModuleGroup(), dep.getModuleName());
            if (!visited.add(key)) {
                continue;
            }
            final AppDependency appDep = versionMap.get(key);
            if (appDep == null) {
                // not a jar
                continue;
            }

            final Dependency extDep;
            final Path artifactPath = appDep.getArtifact().getPath();
            if (Files.isDirectory(artifactPath)) {
                extDep = processQuarkusDir(appDep.getArtifact(), artifactPath.resolve(BootstrapConstants.META_INF), appBuilder);
            } else {
                try (FileSystem artifactFs = FileSystems.newFileSystem(artifactPath, null)) {
                    extDep = processQuarkusDir(appDep.getArtifact(), artifactFs.getPath(BootstrapConstants.META_INF),
                            appBuilder);
                } catch (IOException e) {
                    throw new GradleException("Failed to process " + artifactPath, e);
                }
            }

            if (extDep != null && firstLevelExt) {
                firstLevelExt = false;
                firstLevelExtensions.add(extDep);
            }
            final Set<ResolvedDependency> resolvedChildren = dep.getChildren();
            if (!resolvedChildren.isEmpty()) {
                collectExtensionDeps(resolvedChildren, versionMap, appBuilder, firstLevelExtensions, firstLevelExt, visited);
            }
        }
    }

    /**
     * A {@link ResolvedArtifact} is valid if it's a JAR or a directory
     */
    private static boolean isDependency(ResolvedArtifact a) {
        return BootstrapConstants.JAR.equalsIgnoreCase(a.getExtension()) ||
                a.getFile().isDirectory();
    }

    private AppDependency alignVersion(AppDependency dependency, Map<AppArtifactKey, AppDependency> versionMap) {
        AppArtifactKey appKey = new AppArtifactKey(dependency.getArtifact().getGroupId(),
                dependency.getArtifact().getArtifactId());
        if (versionMap.containsKey(appKey)) {
            return versionMap.get(appKey);
        }
        return dependency;
    }

    @Override
    public AppModel resolveModel(AppArtifact root, List<AppDependency> deps) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppModel resolveManagedModel(AppArtifact appArtifact, List<AppDependency> directDeps, AppArtifact managingProject)
            throws AppModelResolverException {
        return resolveModel(appArtifact);
    }

    private static ModuleIdentifier getModuleId(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        return DefaultModuleIdentifier.newId(split[0], split[1]);
    }

    static AppDependency toAppDependency(ResolvedArtifact a) {
        return new AppDependency(toAppArtifact(a), "runtime");
    }

    public static AppArtifact toAppArtifact(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        final AppArtifact appArtifact = new AppArtifact(split[0], split[1], split.length > 2 ? split[2] : null);
        appArtifact.setPath(a.getFile().toPath());
        return appArtifact;
    }

    private Dependency processQuarkusDir(AppArtifact a, Path quarkusDir, AppModel.Builder appBuilder) {
        if (!Files.exists(quarkusDir)) {
            return null;
        }
        final Path quarkusDescr = quarkusDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
        if (!Files.exists(quarkusDescr)) {
            return null;
        }
        final Properties extProps = resolveDescriptor(quarkusDescr);
        if (extProps == null) {
            return null;
        }
        appBuilder.handleExtensionProperties(extProps, a.toString());
        String value = extProps.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        final String[] split = value.split(":");

        return new DefaultExternalModuleDependency(split[0], split[1], split[2], null);
    }

    private Properties resolveDescriptor(final Path path) {
        final Properties rtProps;
        if (!Files.exists(path)) {
            // not a platform artifact
            return null;
        }
        rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new GradleException("Failed to load extension description " + path, e);
        }
        return rtProps;
    }
}
