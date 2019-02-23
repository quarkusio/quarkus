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

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact {

    private static final String CLASSIFIER_NONE = "";
    private static final String TYPE_JAR = "jar";

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;
    protected Path path;

    public AppArtifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, CLASSIFIER_NONE, TYPE_JAR, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String version) {
        this(groupId, artifactId, classifier, TYPE_JAR, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? CLASSIFIER_NONE : classifier;
        this.type = type;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public boolean hasClassifier() {
        return !classifier.isEmpty();
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public Path getPath() {
        return path;
    }

    protected void setPath(Path path) {
        this.path = path;
    }

    public boolean isResolved() {
        return path != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AppArtifact other = (AppArtifact) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(128);
        buf.append(groupId).append(':').append(artifactId).append(':').append(classifier).append(':').append(type).append(':')
                .append(version);
        return buf.toString();
    }
}
