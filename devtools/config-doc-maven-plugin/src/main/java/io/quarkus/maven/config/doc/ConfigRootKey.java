package io.quarkus.maven.config.doc;

import java.util.Objects;
import java.util.regex.Pattern;

import io.quarkus.annotation.processor.documentation.config.util.Markers;

class ConfigRootKey {

    private final String groupId;
    private final String artifactId;
    private final String topLevelPrefix;

    public ConfigRootKey(String groupId, String artifactId, String prefix) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.topLevelPrefix = buildTopLevelPrefix(prefix);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getTopLevelPrefix() {
        return topLevelPrefix;
    }

    private static String buildTopLevelPrefix(String prefix) {
        String[] prefixSegments = prefix.split(Pattern.quote(Markers.DOT));

        if (prefixSegments.length == 1) {
            return prefixSegments[0];
        }

        return prefixSegments[0] + Markers.DOT + prefixSegments[1];
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, groupId, topLevelPrefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConfigRootKey other = (ConfigRootKey) obj;
        return Objects.equals(artifactId, other.artifactId) && Objects.equals(groupId, other.groupId)
                && Objects.equals(topLevelPrefix, other.topLevelPrefix);
    }

    @Override
    public String toString() {
        return "ConfigRootKey [groupId=" + groupId + ", artifactId=" + artifactId + ", topLevelPrefix=" + topLevelPrefix
                + "]";
    }
}