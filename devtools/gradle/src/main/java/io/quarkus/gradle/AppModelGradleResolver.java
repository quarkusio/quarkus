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
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.gradle.api.provider.Provider;
import org.gradle.jvm.tasks.Jar;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
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
    public String getLatestVersion(AppArtifact arg0, String arg1, boolean arg2) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextVersion(AppArtifact arg0, String fromVersion, boolean fromVersionIncluded, String arg1, boolean arg2)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listLaterVersions(AppArtifact arg0, String arg1, boolean arg2) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void relink(AppArtifact arg0, Path arg1) throws AppModelResolverException {

    }

    @Override
    public Path resolve(AppArtifact appArtifact) throws AppModelResolverException {
        if (!appArtifact.isResolved()) {
            throw new AppModelResolverException("Artifact has not been resolved: " + appArtifact);
        }
        return appArtifact.getPath();
    }

    @Override
    public List<AppDependency> readManagedDependencies(AppArtifact artifact) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact appArtifact, List<AppDependency> directDeps)
            throws AppModelResolverException {
        return Collections.emptyList();
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact) throws AppModelResolverException {
        if (appModel != null && appModel.getAppArtifact().equals(appArtifact)) {
            return appModel;
        }
        final Configuration compileCp = project.getConfigurations().getByName("compileClasspath");
        final List<Dependency> extensionDeps = new ArrayList<>();
        final List<AppDependency> userDeps = new ArrayList<>();
        Map<ModuleIdentifier, ModuleVersionIdentifier> userModules = new HashMap<>();
        for (ResolvedArtifact a : compileCp.getResolvedConfiguration().getResolvedArtifacts()) {
            final File f = a.getFile();

            if (!"jar".equals(a.getExtension()) && !f.isDirectory()) {
                continue;
            }

            userModules.put(getModuleId(a), a.getModuleVersion().getId());
            userDeps.add(toAppDependency(a));

            final Dependency dep;
            if (f.isDirectory()) {
                dep = processQuarkusDir(a, f.toPath().resolve(BootstrapConstants.META_INF));
            } else {
                try (FileSystem artifactFs = FileSystems.newFileSystem(f.toPath(), null)) {
                    dep = processQuarkusDir(a, artifactFs.getPath(BootstrapConstants.META_INF));
                } catch (IOException e) {
                    throw new GradleException("Failed to process " + f, e);
                }
            }
            if (dep != null) {
                extensionDeps.add(dep);
            }
        }
        List<AppDependency> deploymentDeps = new ArrayList<>();
        if (!extensionDeps.isEmpty()) {
            final Configuration deploymentConfig = project.getConfigurations()
                    .detachedConfiguration(extensionDeps.toArray(new Dependency[extensionDeps.size()]));
            final ResolvedConfiguration rc = deploymentConfig.getResolvedConfiguration();
            for (ResolvedArtifact a : rc.getResolvedArtifacts()) {
                final ModuleVersionIdentifier userVersion = userModules.get(getModuleId(a));
                if (userVersion != null) {
                    continue;
                }
                deploymentDeps.add(toAppDependency(a));
            }
        }

        /*
         * System.out.println("USER APP DEPENDENCIES");
         * for (AppDependency dep : userDeps) {
         * System.out.println(" " + dep);
         * }
         * System.out.println("DEPLOYMENT DEPENDENCIES");
         * for (AppDependency dep : deploymentDeps) {
         * System.out.println(" " + dep);
         * }
         */

        // In the case of quarkusBuild (which is the primary user of this),
        // it's not necessary to actually resolve the original application JAR
        if (!appArtifact.isResolved()) {
            final Jar jarTask = (Jar) project.getTasks().findByName("jar");
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
        return this.appModel = new AppModel(appArtifact, userDeps, deploymentDeps);
    }

    @Override
    public AppModel resolveModel(AppArtifact arg0, List<AppDependency> arg1) throws AppModelResolverException {
        throw new UnsupportedOperationException();
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

    private Dependency processQuarkusDir(ResolvedArtifact a, Path quarkusDir) {
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
        String value = extProps.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        final String[] split = value.split(":");

        return new QuarkusExtDependency(split[0], split[1], split[2], null);
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
