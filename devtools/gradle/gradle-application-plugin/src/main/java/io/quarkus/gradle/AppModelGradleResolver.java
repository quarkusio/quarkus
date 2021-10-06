package io.quarkus.gradle;

import java.nio.file.Path;
import java.util.Collection;
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

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.maven.dependency.ResolvedDependency;

public class AppModelGradleResolver implements AppModelResolver {

    private final Project project;
    private final ApplicationModel model;

    public AppModelGradleResolver(Project project, ApplicationModel model) {
        this.model = model;
        this.project = project;
    }

    @Override
    public String getLatestVersion(ArtifactCoords appArtifact, String upToVersion,
            boolean inclusive)
            throws AppModelResolverException {
        try {
            return resolveArtifact(
                    new GACTV(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                            appArtifact.getClassifier(), appArtifact.getType(),
                            "[" + appArtifact.getVersion() + "," + upToVersion + (inclusive ? "]" : ")")))
                                    .getVersion();
        } catch (AppModelResolverException e) {
            return null;
        }
    }

    @Override
    public String getLatestVersionFromRange(ArtifactCoords appArtifact, String range)
            throws AppModelResolverException {
        try {
            return resolveArtifact(
                    new GACTV(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                            appArtifact.getClassifier(), appArtifact.getType(), range)).getVersion();
        } catch (AppModelResolverException e) {
            return null;
        }
    }

    @Override
    public String getNextVersion(ArtifactCoords appArtifact, String fromVersion,
            boolean fromVersionIncluded, String upToVersion,
            boolean upToVersionIncluded)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listLaterVersions(ArtifactCoords appArtifact, String upToVersion,
            boolean inclusive)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void relink(ArtifactCoords artifact, Path localPath)
            throws AppModelResolverException {

    }

    @Override
    public ResolvedDependency resolve(ArtifactCoords appArtifact) throws AppModelResolverException {
        return resolveArtifact(appArtifact);
    }

    private ResolvedDependency resolveArtifact(
            ArtifactCoords appArtifact) throws AppModelResolverException {
        if (ResolvedDependency.class.isAssignableFrom(appArtifact.getClass())) {
            final ResolvedDependency resolved = (ResolvedDependency) appArtifact;
            if (resolved.isResolved()) {
                return resolved;
            }
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
                final String version = appArtifact.getVersion().equals(a.getModuleVersion().getId().getVersion())
                        ? appArtifact.getVersion()
                        : a.getModuleVersion().getId().getVersion();
                return new ResolvedArtifactDependency(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                        appArtifact.getClassifier(), appArtifact.getType(), version, a.getFile().toPath());
            }
        }
        throw new AppModelResolverException("Failed to resolve " + appArtifact);
    }

    @Override
    public Collection<ResolvedDependency> resolveUserDependencies(ArtifactCoords appArtifact,
            Collection<Dependency> directDeps) {
        return Collections.emptyList();
    }

    @Override
    public ApplicationModel resolveModel(ArtifactCoords appArtifact)
            throws AppModelResolverException {
        if (model.getAppArtifact().toGACTVString().equals(appArtifact.toGACTVString())) {
            return model;
        }
        throw new AppModelResolverException(
                "Requested artifact " + appArtifact + " does not match loaded model " + model.getAppArtifact());
    }

    @Override
    public ApplicationModel resolveModel(ArtifactCoords root, Collection<Dependency> deps)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApplicationModel resolveManagedModel(ArtifactCoords appArtifact,
            Collection<Dependency> directDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> localProjects)
            throws AppModelResolverException {
        return resolveModel(appArtifact);
    }
}
