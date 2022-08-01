package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Application model resolver used to resolve application and/or its dependency artifacts.
 *
 * @author Alexey Loubyansky
 */
public interface AppModelResolver {

    /**
     * (Re-)links an artifact to a path.
     *
     * @param artifact an artifact to (re-)link to the path
     * @param localPath local path to the artifact
     * @throws AppModelResolverException in case of a failure
     */
    void relink(ArtifactCoords artifact, Path localPath) throws AppModelResolverException;

    /**
     * Resolves an artifact.
     *
     * @param artifact artifact to resolve
     * @return resolved artifact
     * @throws AppModelResolverException in case of a failure
     */
    ResolvedDependency resolve(ArtifactCoords artifact) throws AppModelResolverException;

    /**
     * Resolve application direct and transitive dependencies configured by the user.
     *
     * Note that deployment dependencies are not included in the result.
     *
     * @param artifact application artifact
     * @return the list of dependencies configured by the user
     * @throws AppModelResolverException in case of a failure
     */
    default Collection<ResolvedDependency> resolveUserDependencies(ArtifactCoords artifact) throws AppModelResolverException {
        return resolveUserDependencies(artifact, Collections.emptyList());
    }

    /**
     * Resolve application direct and transitive dependencies configured by the user,
     * given the specific versions of the direct dependencies.
     *
     * Note that deployment dependencies are not included in the result.
     *
     * @param artifact application artifact
     * @param deps some or all of the direct dependencies that should be used in place of the original ones
     * @return the list of dependencies configured by the user
     * @throws AppModelResolverException in case of a failure
     */
    Collection<ResolvedDependency> resolveUserDependencies(ArtifactCoords artifact, Collection<Dependency> deps)
            throws AppModelResolverException;

    /**
     * Resolve dependencies that are required at runtime, excluding test and optional dependencies.
     *
     * @param artifact
     * @return
     * @throws AppModelResolverException
     */
    ApplicationModel resolveModel(ArtifactCoords artifact) throws AppModelResolverException;

    /**
     * Resolve artifact dependencies given the specific versions of the direct dependencies
     *
     * @param root root artifact
     * @param deps some or all of the direct dependencies that should be used in place of the original ones
     * @return collected dependencies
     * @throws AppModelResolverException in case of a failure
     */
    ApplicationModel resolveModel(ArtifactCoords root, Collection<Dependency> deps) throws AppModelResolverException;

    ApplicationModel resolveManagedModel(ArtifactCoords appArtifact, Collection<Dependency> directDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> localProjects)
            throws AppModelResolverException;

    /**
     * Lists versions released later than the version of the artifact up to the version
     * specified or all the later versions in case the up-to-version is not provided.
     *
     * @param artifact artifact to list the versions for
     * @return the list of versions released later than the version of the artifact
     * @throws AppModelResolverException in case of a failure
     */
    List<String> listLaterVersions(ArtifactCoords artifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException;

    /**
     * Returns the next version of the artifact from the specified range.
     * In case the next version is not available, the method returns null.
     *
     * @param artifact artifact
     * @param fromVersion the lowest version of the range
     * @param fromVersionIncluded whether the specified lowest version should be included in the range
     * @param upToVersion the highest version of the range
     * @param upToVersionIncluded whether the specified highest version should be included in the range
     * @return the next version from the specified range or null if the next version is not available
     * @throws AppModelResolverException in case of a failure
     */
    String getNextVersion(ArtifactCoords artifact, String fromVersion, boolean fromVersionIncluded, String upToVersion,
            boolean upToVersionIncluded) throws AppModelResolverException;

    /**
     * Returns the latest version for the artifact up to the version specified.
     * In case there is no later version available, the artifact's version is returned.
     *
     * @param artifact artifact
     * @param upToVersion max version boundary
     * @param inclusive whether the upToVersion should be included in the range or not
     * @return the latest version up to specified boundary
     * @throws AppModelResolverException in case of a failure
     */
    String getLatestVersion(ArtifactCoords artifact, String upToVersion, boolean inclusive) throws AppModelResolverException;

    /**
     * Resolves the latest version from the specified range. The version of the artifact is ignored.
     *
     * @param appArtifact the artifact
     * @param range the version range
     * @return the latest version of the artifact from the range or null, if no version was found for the specified range
     * @throws AppModelResolverException in case of a failure
     */
    String getLatestVersionFromRange(ArtifactCoords appArtifact, String range) throws AppModelResolverException;
}
