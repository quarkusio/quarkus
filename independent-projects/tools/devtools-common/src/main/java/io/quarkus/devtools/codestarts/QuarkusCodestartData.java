package io.quarkus.devtools.codestarts;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class QuarkusCodestartData {

    private QuarkusCodestartData() {
    }

    public enum DataKey {
        BOM_GROUP_ID("quarkus.platform.group-id"),
        BOM_ARTIFACT_ID("quarkus.platform.artifact-id"),
        BOM_VERSION("quarkus.platform.version"),
        PROJECT_GROUP_ID("project.group-id"),
        PROJECT_ARTIFACT_ID("project.artifact-id"),
        PROJECT_VERSION("project.version"),
        QUARKUS_MAVEN_PLUGIN_GROUP_ID("quarkus.maven-plugin.group-id"),
        QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID("quarkus.maven-plugin.artifact-id"),
        QUARKUS_MAVEN_PLUGIN_VERSION("quarkus.maven-plugin.version"),
        QUARKUS_GRADLE_PLUGIN_ID("quarkus.gradle-plugin.id"),
        QUARKUS_GRADLE_PLUGIN_VERSION("quarkus.gradle-plugin.version"),
        QUARKUS_VERSION("quarkus.version"),
        JAVA_VERSION("java.version"),
        ;

        private final String key;

        DataKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public enum LegacySupport {
        BOM_GROUP_ID("bom_groupId"),
        BOM_ARTIFACT_ID("bom_artifactId"),
        BOM_VERSION("bom_version"),
        PROJECT_GROUP_ID("project_groupId"),
        PROJECT_ARTIFACT_ID("project_artifactId"),
        PROJECT_VERSION("project_version"),
        QUARKUS_MAVEN_PLUGIN_GROUP_ID("maven_plugin_groupId"),
        QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID("maven_plugin_artifactId"),
        QUARKUS_MAVEN_PLUGIN_VERSION("maven_plugin_version"),
        QUARKUS_GRADLE_PLUGIN_ID("gradle_plugin_id"),
        QUARKUS_GRADLE_PLUGIN_VERSION("gradle_plugin_version"),
        QUARKUS_VERSION("quarkus_version"),
        JAVA_VERSION("java_target");

        private final String legacyKey;
        private final String key;

        LegacySupport(String legacyKey) {
            this.key = DataKey.valueOf(this.name()).getKey();
            this.legacyKey = legacyKey;
        }

        public String getKey() {
            return key;
        }

        public String getLegacyKey() {
            return legacyKey;
        }

        public static Map<String, Object> convertFromLegacy(Map<String, Object> legacy) {
            return NestedMaps.unflatten(Stream.of(values())
                    .filter(v -> v.getLegacyKey() != null)
                    .filter(v -> legacy.containsKey(v.getLegacyKey()))
                    .map(v -> new HashMap.SimpleImmutableEntry<>(v.getKey(), legacy.get(v.getLegacyKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }
}
