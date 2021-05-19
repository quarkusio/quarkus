package io.quarkus.gradle;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.QuarkusModelHelper;

public class AppModelGradleResolver implements AppModelResolver {

    private AppModel appModel;
    private final Project project;
    private final QuarkusModel model;

    public AppModelGradleResolver(Project project, QuarkusModel model) {
        this.model = model;
        this.project = project;
    }

    @Override
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        try {
            return resolveArtifact(new AppArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                    appArtifact.getClassifier(), appArtifact.getType(),
                    "[" + appArtifact.getVersion() + "," + upToVersion + (inclusive ? "]" : ")"))).getVersion();
        } catch (AppModelResolverException e) {
            return null;
        }
    }

    @Override
    public String getLatestVersionFromRange(AppArtifact appArtifact, String range) throws AppModelResolverException {
        try {
            return resolveArtifact(new AppArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                    appArtifact.getClassifier(), appArtifact.getType(), range)).getVersion();
        } catch (AppModelResolverException e) {
            return null;
        }
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
        return resolveArtifact(appArtifact).getPaths().getSinglePath();
    }

    private AppArtifact resolveArtifact(AppArtifact appArtifact) throws AppModelResolverException {
        if (appArtifact.isResolved()) {
            return appArtifact;
        }
        final DefaultDependencyArtifact dep = new DefaultDependencyArtifact();
        dep.setExtension(appArtifact.getType());
        dep.setType(appArtifact.getType());
        dep.setName(appArtifact.getArtifactId());
        if (appArtifact.getClassifier() != null) {
            dep.setClassifier(appArtifact.getClassifier());
        }

        final DefaultExternalModuleDependency gradleDep = new DefaultExternalModuleDependency(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getVersion(), null);
        gradleDep.addArtifact(dep);

        final Configuration detachedConfig = project.getConfigurations().detachedConfiguration(gradleDep);

        final ResolvedConfiguration rc = detachedConfig.getResolvedConfiguration();
        Set<ResolvedArtifact> resolvedArtifacts;
        try {
            resolvedArtifacts = rc.getResolvedArtifacts();
        } catch (ResolveException e) {
            throw new AppModelResolverException("Failed to resolve " + appArtifact, e);
        }
        for (ResolvedArtifact a : resolvedArtifacts) {
            if (appArtifact.getArtifactId().equals(a.getName()) && appArtifact.getType().equals(a.getType())
                    && (a.getClassifier() == null ? appArtifact.getClassifier() == null
                            : a.getClassifier().equals(appArtifact.getClassifier()))
                    && appArtifact.getGroupId().equals(a.getModuleVersion().getId().getGroup())) {
                if (!appArtifact.getVersion().equals(a.getModuleVersion().getId().getVersion())) {
                    appArtifact = new AppArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                            appArtifact.getClassifier(), appArtifact.getType(), a.getModuleVersion().getId().getVersion());
                }
                appArtifact.setPath(a.getFile().toPath());
                break;
            }
        }

        if (!appArtifact.isResolved()) {
            throw new AppModelResolverException("Failed to resolve " + appArtifact);
        }
        return appArtifact;
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact appArtifact, List<AppDependency> directDeps) {
        return Collections.emptyList();
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact) throws AppModelResolverException {
        if (appModel != null) {
            if (appModel.getAppArtifact().equals(appArtifact)) {
                return appModel;
            } else {
                throw new AppModelResolverException(
                        "Requested artifact : " + appArtifact + ", does not match loaded model " + appModel.getAppArtifact());
            }
        }
        appModel = QuarkusModelHelper.convert(model, appArtifact);
        return appModel;
    }

    @Override
    public AppModel resolveModel(AppArtifact root, List<AppDependency> deps) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppModel resolveManagedModel(AppArtifact appArtifact, List<AppDependency> directDeps, AppArtifact managingProject,
            Set<AppArtifactKey> localProjects)
            throws AppModelResolverException {
        return resolveModel(appArtifact);
    }
}
