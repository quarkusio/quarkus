/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator;

import java.nio.file.Path;
import java.util.List;

/**
 * Artifact resolver used to resolve application and/or its dependency artifacts.
 *
 * @author Alexey Loubyansky
 */
public interface AppArtifactResolver {

    /**
     * (Re-)links an artifact to a path.
     *
     * @param appArtifact an artifact to (re-)link to the path
     * @param localPath local path to the artifact
     * @throws AppCreatorException in case of a failure
     */
    void relink(AppArtifact appArtifact, Path localPath) throws AppCreatorException;

    /**
     * Resolves an artifact.
     *
     * @param artifact artifact to resolve
     * @return local path
     * @throws AppCreatorException in case of a failure
     */
    Path resolve(AppArtifact artifact) throws AppCreatorException;

    /**
     * Collects all the artifact dependencies.
     *
     * @param artifact root artifact
     * @return collected dependencies
     * @throws AppCreatorException in case of a failure
     */
    List<AppDependency> collectDependencies(AppArtifact artifact) throws AppCreatorException;

    /**
     * Collects artifact dependencies merging the provided direct dependencies in
     *
     * @param root root artifact
     * @param deps some or all of the direct dependencies that should be used in place of the original ones
     * @return collected dependencies
     * @throws AppCreatorException in case of a failure
     */
    List<AppDependency> collectDependencies(AppArtifact root, List<AppDependency> deps) throws AppCreatorException;

    /**
     * Lists versions released later than the version of the artifact up to the version
     * specified or all the later versions in case the up-to-version is not provided.
     *
     * @param artifact artifact to list the versions for
     * @return the list of versions released later than the version of the artifact
     * @throws AppCreatorException in case of a failure
     */
    List<String> listLaterVersions(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException;

    /**
     * Returns the next version for the artifact which is not later than the version specified.
     * In case the next version is not available, the artifact's version is returned.
     *
     * @param artifact artifact
     * @param upToVersion max version boundary
     * @param inclusive whether the upToVersion should be included in the range or not
     * @return the next version which is not later than the specified boundary
     * @throws AppCreatorException in case of a failure
     */
    String getNextVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException;

    /**
     * Returns the latest version for the artifact up to the version specified.
     * In case there is no later version available, the artifact's version is returned.
     *
     * @param artifact artifact
     * @param upToVersion max version boundary
     * @param inclusive whether the upToVersion should be included in the range or not
     * @return the latest version up to specified boundary
     * @throws AppCreatorException in case of a failure
     */
    String getLatestVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException;
}
