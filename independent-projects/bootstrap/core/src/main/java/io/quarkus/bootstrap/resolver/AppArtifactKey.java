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

package io.quarkus.bootstrap.resolver;

/**
 * GroupId, artifactId and classifier
 *
 * @author Alexey Loubyansky
 */
public class AppArtifactKey {

    public static AppArtifactKey fromString(String str) {
        return new AppArtifactKey(split(str, new String[3], str.length()));
    }

    protected static String[] split(String str, String[] parts, int fromIndex) {
        int i = str.lastIndexOf(':', fromIndex - 1);
        if(i <= 0) {
            throw new IllegalArgumentException("GroupId and artifactId separating ':' is abscent or not in the right place in '" + str.substring(0, fromIndex) + "'");
        }
        parts[2] = str.substring(i + 1, fromIndex);
        fromIndex = i;
        i = str.lastIndexOf(':', fromIndex - 1);
        if(i < 0) {
            parts[0] = str.substring(0, fromIndex);
            if((parts[1] = parts[2]).isEmpty()) {
                throw new IllegalArgumentException("ArtifactId is empty in `" + str + "`");
            }
            parts[2] = "";
        } else if(i == 0 || i == fromIndex - 1) {
            throw new IllegalArgumentException("One of groupId or artifactId is missing from '" + str.substring(0, fromIndex) + "'");
        } else {
            parts[0] = str.substring(0, i);
            parts[1] = str.substring(i + 1, fromIndex);
        }
        return parts;
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;

    protected AppArtifactKey(String[] parts) {
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.classifier = parts[2];
    }

    public AppArtifactKey(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public AppArtifactKey(String groupId, String artifactId, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
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
        AppArtifactKey other = (AppArtifactKey) obj;
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
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        if(!classifier.isEmpty()) {
            buf.append(':').append(classifier);
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        AppArtifactKey ga = fromString("g:a:c");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
        System.out.println("'" + ga.getClassifier() + "'");
    }
}
