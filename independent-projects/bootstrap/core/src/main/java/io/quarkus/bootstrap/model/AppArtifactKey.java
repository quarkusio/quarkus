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
 * GroupId, artifactId and classifier
 *
 * @author Alexey Loubyansky
 */
public class AppArtifactKey {

    public static AppArtifactKey fromString(String str) {
        return new AppArtifactKey(split(str, new String[4], str.length()));
    }

    protected static String[] split(String str, String[] parts, int fromIndex) {
        int i = str.lastIndexOf(':', fromIndex - 1);
        if(i <= 0) {
            throw new IllegalArgumentException("GroupId and artifactId separating ':' is abscent or not in the right place in '" + str.substring(0, fromIndex) + "'");
        }
        parts[3] = str.substring(i + 1, fromIndex);
        fromIndex = i;
        i = str.lastIndexOf(':', fromIndex - 1);
        if(i < 0) {
            parts[0] = str.substring(0, fromIndex);
            if((parts[1] = parts[3]).isEmpty()) {
                throw new IllegalArgumentException("ArtifactId is empty in `" + str + "`");
            }
            parts[2] = "";
            parts[3] = null;
            return parts;
        }
        if(i == 0) {
            throw new IllegalArgumentException("One of groupId or artifactId is missing from '" + str.substring(0, fromIndex) + "'");
        }
        if(i == fromIndex - 1) {
            parts[2] = "";
        } else {
            parts[2] = str.substring(i + 1, fromIndex);
        }

        fromIndex = i;
        i = str.lastIndexOf(':', fromIndex - 1);
        if(i < 0) {
            parts[0] = str.substring(0, fromIndex);
            if((parts[1] = parts[2]).isEmpty()) {
                throw new IllegalArgumentException("ArtifactId is empty in `" + str + "`");
            }
            parts[2] = parts[3];
            parts[3] = null;
            return parts;
        }
        if(i == 0 || i == fromIndex - 1) {
            throw new IllegalArgumentException("One of groupId or artifactId is missing from '" + str.substring(0, fromIndex) + "'");
        }

        parts[0] = str.substring(0, i);
        parts[1] = str.substring(i + 1, fromIndex);
        if(parts[3].isEmpty()) {
            parts[3] = null;
        }
        return parts;
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;

    protected AppArtifactKey(String[] parts) {
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.classifier = parts[2];
        this.type = parts[3];
    }

    public AppArtifactKey(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public AppArtifactKey(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, null);
    }

    public AppArtifactKey(String groupId, String artifactId, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
        this.type = type;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        if(!classifier.isEmpty()) {
            buf.append(':').append(classifier);
        } else if(type != null) {
            buf.append(':');
        }
        if(type != null) {
            buf.append(':').append(type);
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        AppArtifactKey ga = fromString("g:a:c:t");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
        System.out.println("'" + ga.getClassifier() + "'");
        System.out.println(ga.getType());
    }
}
