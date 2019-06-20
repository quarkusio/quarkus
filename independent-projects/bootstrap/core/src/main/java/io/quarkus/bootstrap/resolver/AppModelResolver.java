package io.quarkus.bootstrap.resolver;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;

/**
 * Application model resolver used to resolve application and/or its dependency artifacts.
 *
 * @author Alexey Loubyansky
 */
public interface AppModelResolver {

    /**
     * (Re-)links an artifact to a path.
     *
     * @param appArtifact  an artifact to (re-)link to the path
     * @param localPath  local path to the artifact
     * @throws AppModelResolverException  in case of a failure
     */
    void relink(AppArtifact appArtifact, Path localPath) throws AppModelResolverException;

    /**
     * Resolves an artifact.
     *
     * @param artifact  artifact to resolve
     * @return  local path
     * @throws AppModelResolverException  in case of a failure
     */
    Path resolve(AppArtifact artifact) throws AppModelResolverException;

    /**
     * Returns dependency versions that are forced by the artifact
     * on its dependencies.
     *
     * @param artifact  target artifact
     * @return  list of dependency versions forced by the artifact on its dependencies
     * @throws AppModelResolverException
     */
    List<AppDependency> readManagedDependencies(AppArtifact artifact) throws AppModelResolverException;

    /**
     * Resolve application direct and transitive dependencies configured by the user.
     *
     * Note that deployment dependencies are not included in the result.
     *
     * @param artifact  application artifact
     * @return  the list of dependencies configured by the user
     * @throws AppModelResolverException  in case of a failure
     */
    default List<AppDependency> resolveUserDependencies(AppArtifact artifact) throws AppModelResolverException {
        return resolveUserDependencies(artifact, Collections.emptyList());
    }

    /**
     * Resolve application direct and transitive dependencies configured by the user,
     * given the specific versions of the direct dependencies.
     *
     * Note that deployment dependencies are not included in the result.
     *
     * @param artifact  application artifact
     * @param deps  some or all of the direct dependencies that should be used in place of the original ones
     * @return  the list of dependencies configured by the user
     * @throws AppModelResolverException  in case of a failure
     */
    List<AppDependency> resolveUserDependencies(AppArtifact artifact, List<AppDependency> deps) throws AppModelResolverException;

    /**
     * Resolve dependencies that are required at runtime, excluding test and optional dependencies.
     *
     * @param artifact
     * @return
     * @throws AppModelResolverException
     */
    AppModel resolveModel(AppArtifact artifact) throws AppModelResolverException;

    /**
     * Resolve artifact dependencies given the specific versions of the direct dependencies
     *
     * @param root  root artifact
     * @param deps  some or all of the direct dependencies that should be used in place of the original ones
     * @return  collected dependencies
     * @throws AppModelResolverException  in case of a failure
     */
    AppModel resolveModel(AppArtifact root, List<AppDependency> deps) throws AppModelResolverException;

    /**
     * Lists versions released later than the version of the artifact up to the version
     * specified or all the later versions in case the up-to-version is not provided.
     *
     * @param artifact  artifact to list the versions for
     * @return  the list of versions released later than the version of the artifact
     * @throws AppModelResolverException  in case of a failure
     */
    List<String> listLaterVersions(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppModelResolverException;

    /**
     * Returns the next version of the artifact from the specified range.
     * In case the next version is not available, the method returns null.
     *
     * @param artifact  artifact
     * @param fromVersion  the lowest version of the range
     * @param fromVersionIncluded  whether the specified lowest version should be included in the range
     * @param upToVersion  the highest version of the range
     * @param upToVersionIncluded  whether the specified highest version should be included in the range
     * @return  the next version from the specified range or null if the next version is not avaiable
     * @throws AppModelResolverException  in case of a failure
     */
    String getNextVersion(AppArtifact artifact, String fromVersion, boolean fromVersionIncluded, String upToVersion, boolean upToVersionIncluded) throws AppModelResolverException;

    /**
     * Returns the latest version for the artifact up to the version specified.
     * In case there is no later version available, the artifact's version is returned.
     *
     * @param artifact  artifact
     * @param upToVersion  max version boundary
     * @param inclusive  whether the upToVersion should be included in the range or not
     * @return  the latest version up to specified boundary
     * @throws AppModelResolverException  in case of a failure
     */
    String getLatestVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppModelResolverException;
}
