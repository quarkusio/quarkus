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

package org.jboss.shamrock.bootstrap.resolver;

/**
 * GroupId, artifactId and classifier
 *
 * @author Alexey Loubyansky
 */
public class GAC extends GA {

    public static GAC fromString(String str) {
        return new GAC(split(str, new String[3], str.length()));
    }

    protected static String[] split(String str, String[] parts, int fromIndex) {
        final int i = str.lastIndexOf(':', fromIndex - 1);
        if(i <= 0) {
            throw new IllegalArgumentException("ArtifactId and classifier separating ':' is abscent or not in the right place in '" + str.substring(0, fromIndex) + "'");
        }
        GA.split(str, parts, i);
        parts[2] = str.substring(i + 1, fromIndex);
        return parts;
    }

    protected final String classifier;

    protected GAC(String[] parts) {
        super(parts);
        this.classifier = parts[2];
    }

    public GAC(String groupId, String artifactId, String classifier) {
        super(groupId, artifactId);
        this.classifier = classifier == null ? "" : classifier;
    }

    public String getClassifier() {
        return classifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GAC other = (GAC) obj;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        return true;
    }

    @Override
    protected StringBuilder append(StringBuilder buf) {
        return super.append(buf).append(':').append(classifier);
    }

    public static void main(String[] args) {
        GAC ga = fromString("g:a:");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
        System.out.println("'" + ga.getClassifier() + "'");
    }
}
