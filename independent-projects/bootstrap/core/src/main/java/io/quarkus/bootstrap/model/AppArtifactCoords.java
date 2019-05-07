/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.bootstrap.model;

/**
 * GroupId, artifactId, classifier, type, version
 *
 * @author Alexey Loubyansky
 */
public class AppArtifactCoords {

    public static final String TYPE_JAR = "jar";
    public static final String TYPE_POM = "pom";

    public static AppArtifactCoords fromString(String str) {
        return new AppArtifactCoords(split(str, new String[5]));
    }

    protected static String[] split(String str, String[] parts) {
        final int versionSep = str.lastIndexOf(':');
        if(versionSep <= 0 || versionSep == str.length() - 1) {
            throw new IllegalArgumentException("One of type, version or separating them ':' is missing from '" + str + "'");
        }
        parts[4] = str.substring(versionSep + 1);
        AppArtifactKey.split(str, parts, versionSep);
        return parts;
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;

    protected AppArtifactKey key;

    protected AppArtifactCoords(String[] parts) {
        groupId = parts[0];
        artifactId = parts[1];
        classifier = parts[2];
        type = parts[3] == null ? TYPE_JAR : parts[3];
        version = parts[4];
    }

    public AppArtifactCoords(String groupId, String artifactId, String version) {
        this(groupId, artifactId, "", TYPE_JAR, version);
    }

    public AppArtifactCoords(String groupId, String artifactId, String type, String version) {
        this(groupId, artifactId, "", type, version);
    }

    public AppArtifactCoords(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
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

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public AppArtifactKey getKey() {
        return key == null ? key = new AppArtifactKey(groupId, artifactId, classifier, type) : key;
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
        AppArtifactCoords other = (AppArtifactCoords) obj;
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
        final StringBuilder buf = new StringBuilder();
        append(buf);
        return buf.toString();
    }

    protected StringBuilder append(final StringBuilder buf) {
        buf.append(groupId).append(':').append(artifactId).append(':');
        if(!classifier.isEmpty()) {
            buf.append(classifier);
        }
        return buf.append(':').append(type).append(':').append(version);
    }

    public static void main(String[] args) {
        AppArtifactCoords ga = fromString("g:a:v");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
        System.out.println("'" + ga.getClassifier() + "'");
        System.out.println(ga.getType());
        System.out.println(ga.getVersion());
    }
}
