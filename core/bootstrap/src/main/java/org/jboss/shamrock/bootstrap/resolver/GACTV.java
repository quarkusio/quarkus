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
 * GroupId, artifactId, classifier, type, version
 *
 * @author Alexey Loubyansky
 */
public class GACTV extends GAC {

    public static GACTV fromString(String str) {
        return new GACTV(split(str, new String[5]));
    }

    protected static String[] split(String str, String[] parts) {
        final int versionSep = str.lastIndexOf(':');
        if(versionSep <= 0 || versionSep == str.length() - 1) {
            throw new IllegalArgumentException("One of type, version or separating them ':' is missing from '" + str + "'");
        }
        final int typeSep = str.lastIndexOf(':', versionSep - 1);
        if(typeSep <= 0 || typeSep == versionSep - 1) {
            throw new IllegalArgumentException("One of classifier, type or separating them ':' is missing from '" + str + "'");
        }
        GAC.split(str, parts, typeSep);
        parts[3] = str.substring(typeSep + 1, versionSep);
        parts[4] = str.substring(versionSep + 1);
        return parts;
    }

    protected final String type;
    protected final String version;

    protected GACTV(String[] parts) {
        super(parts);
        type = parts[3];
        version = parts[4];
    }

    public GACTV(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier);
        this.type = type;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        GACTV other = (GACTV) obj;
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
    protected StringBuilder append(StringBuilder buf) {
        return super.append(buf).append(':').append(type).append(':').append(version);
    }

    public static void main(String[] args) {
        GACTV ga = fromString("g:a:c:t:v");
        System.out.println(ga.getGroupId());
        System.out.println(ga.getArtifactId());
        System.out.println("'" + ga.getClassifier() + "'");
        System.out.println(ga.getType());
        System.out.println(ga.getVersion());
    }
}
