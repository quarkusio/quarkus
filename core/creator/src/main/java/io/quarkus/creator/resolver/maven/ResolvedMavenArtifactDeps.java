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

package io.quarkus.creator.resolver.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolverBase;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedMavenArtifactDeps extends AppArtifactResolverBase {

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String type;
    private final String version;
    private final List<AppDependency> deps;

    public ResolvedMavenArtifactDeps(String groupId, String artifactId, String version, Collection<Artifact> artifacts) {
        this(groupId, artifactId, "", version, artifacts);
    }

    public ResolvedMavenArtifactDeps(String groupId, String artifactId, String classifier, String version,
            Collection<Artifact> artifacts) {
        this(groupId, artifactId, "", "jar", version, artifacts);
    }

    public ResolvedMavenArtifactDeps(String groupId, String artifactId, String classifier, String type, String version,
            Collection<Artifact> artifacts) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.type = type;
        this.version = version;
        final List<AppDependency> tmp = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            tmp.add(new AppDependency(toMvnArtifact(artifact), artifact.getScope()));
        }
        deps = Collections.unmodifiableList(tmp);
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doResolve(AppArtifact coords) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact coords) throws AppCreatorException {
        if (!coords.getGroupId().equals(groupId) ||
                !coords.getArtifactId().equals(artifactId) ||
                !coords.getClassifier().equals(classifier) ||
                !coords.getType().equals(type) ||
                !coords.getVersion().equals(version)) {
            throw new AppCreatorException("The resolve can only resolve dependencies for " + groupId + ':' + artifactId + ':'
                    + classifier + ':' + type + ':' + version + ": " + coords);
        }
        return deps;
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact root, List<AppDependency> deps) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    private static AppArtifact toMvnArtifact(Artifact artifact) {
        final AppArtifact mvn = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(), artifact.getVersion());
        final File file = artifact.getFile();
        if (file != null) {
            setPath(mvn, file.toPath());
        }
        return mvn;
    }

    @Override
    public List<String> listLaterVersions(AppArtifact artifact, String upToVersion, boolean inclusive)
            throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }
}
