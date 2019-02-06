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
 * GroupId and artifactId
 *
 * @author Alexey Loubyansky
 */
public class GA {

    public static GA fromString(String str) {
        return new GA(split(str, new String[2], str.length()));
    }

    protected static String[] split(String str, String[] parts, int fromIndex) {
        final int i = str.lastIndexOf(':', fromIndex- 1);
        if(i <= 0 || i == fromIndex - 1) {
            throw new IllegalArgumentException("One of groupId, artifactId or separating them ':' is missing from '" + str.substring(0, fromIndex) + "'");
        }
        parts[0] = str.substring(0, i);
        parts[1] = str.substring(i + 1, fromIndex);
        return parts;
    }

    protected final String groupId;
    protected final String artifactId;

    protected GA(String[] parts) {
        groupId = parts[0];
        artifactId = parts[1];
    }

    public GA(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
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
        GA other = (GA) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
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
        return append(new StringBuilder()).toString();
    }

    protected StringBuilder append(StringBuilder buf) {
        return buf.append(groupId).append(':').append(artifactId);
    }

    public static void main(String[] args) {
        GA ga = fromString("g:a");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
    }
}
