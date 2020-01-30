package io.quarkus.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.tasks.Jar;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;

public class AppModelGradleResolver implements AppModelResolver {

    private AppModel appModel;

    private final Project project;

    public AppModelGradleResolver(Project project) {
        this.project = project;
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
        final Configuration compileCp = project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        final List<Dependency> extensionDeps = new ArrayList<>();
        final List<AppDependency> userDeps = new ArrayList<>();
        Map<AppArtifactKey, AppDependency> versionMap = new HashMap<>();
        Map<ModuleIdentifier, ModuleVersionIdentifier> userModules = new HashMap<>();
        for (ResolvedArtifact a : compileCp.getResolvedConfiguration().getResolvedArtifacts()) {
            final File f = a.getFile();

            if (!"jar".equals(a.getExtension()) && !f.isDirectory()) {
                continue;
            }

            userModules.put(getModuleId(a), a.getModuleVersion().getId());
            AppDependency dependency = toAppDependency(a);
            userDeps.add(dependency);
            versionMap.put(new AppArtifactKey(dependency.getArtifact().getGroupId(), dependency.getArtifact().getArtifactId(),
                    dependency.getArtifact().getClassifier()), dependency);

            final Dependency dep;
            if (f.isDirectory()) {
                dep = processQuarkusDir(a, f.toPath().resolve(BootstrapConstants.META_INF), appBuilder);
            } else {
                try (FileSystem artifactFs = FileSystems.newFileSystem(f.toPath(), null)) {
                    dep = processQuarkusDir(a, artifactFs.getPath(BootstrapConstants.META_INF), appBuilder);
                } catch (IOException e) {
                    throw new GradleException("Failed to process " + f, e);
                }
            }
            if (dep != null) {
                extensionDeps.add(dep);
            }
        }
        List<AppDependency> deploymentDeps = new ArrayList<>();
        List<AppDependency> fullDeploymentDeps = new ArrayList<>();
        if (!extensionDeps.isEmpty()) {
            final Configuration deploymentConfig = project.getConfigurations()
                    .detachedConfiguration(extensionDeps.toArray(new Dependency[extensionDeps.size()]));
            final ResolvedConfiguration rc = deploymentConfig.getResolvedConfiguration();
            for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
                final ModuleVersionIdentifier userVersion = userModules.get(getModuleId(a));
                if (userVersion != null) {
                    continue;
                }
                AppDependency dependency = toAppDependency(a);
                deploymentDeps.add(alignVersion(dependency, versionMap));
            }
        }
        fullDeploymentDeps.addAll(deploymentDeps);
        fullDeploymentDeps.addAll(userDeps);

        Iterator<AppDependency> it = deploymentDeps.iterator();
        while (it.hasNext()) {
            AppDependency val = it.next();
            if (userDeps.contains(val)) {
                it.remove();
            }
        }

        // In the case of quarkusBuild (which is the primary user of this),
        // it's not necessary to actually resolve the original application JAR
        if (!appArtifact.isResolved()) {
            final Jar jarTask = (Jar) project.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
            if (jarTask == null) {
                throw new AppModelResolverException("Failed to locate task 'jar' in the project.");
            }
            final Provider<RegularFile> jarProvider = jarTask.getArchiveFile();
            if (jarProvider.isPresent()) {
                final File f = jarProvider.get().getAsFile();
                if (f.exists()) {
                    appArtifact.setPath(f.toPath());
                }
            }
        }
        appBuilder.addRuntimeDeps(userDeps)
                .addFullDeploymentDeps(fullDeploymentDeps)
                .addDeploymentDeps(deploymentDeps)
                .setAppArtifact(appArtifact);
        return this.appModel = appBuilder.build();
    }

    private AppDependency alignVersion(AppDependency dependency, Map<AppArtifactKey, AppDependency> versionMap) {
        AppArtifactKey appKey = new AppArtifactKey(dependency.getArtifact().getGroupId(),
                dependency.getArtifact().getArtifactId(),
                dependency.getArtifact().getClassifier());
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

    private ModuleIdentifier getModuleId(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        return DefaultModuleIdentifier.newId(split[0], split[1]);
    }

    private AppDependency toAppDependency(ResolvedArtifact a) {
        final String[] split = a.getModuleVersion().toString().split(":");
        final AppArtifact appArtifact = new AppArtifact(split[0], split[1], split[2]);
        appArtifact.setPath(a.getFile().toPath());
        return new AppDependency(appArtifact, "runtime");
    }

    private Dependency processQuarkusDir(ResolvedArtifact a, Path quarkusDir, AppModel.Builder appBuilder) {
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
