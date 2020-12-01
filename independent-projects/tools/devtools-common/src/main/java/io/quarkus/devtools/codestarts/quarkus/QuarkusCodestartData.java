package io.quarkus.devtools.codestarts.quarkus;

import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
        PROJECT_PACKAGE_NAME("project.package-name"),
        QUARKUS_MAVEN_PLUGIN_GROUP_ID("quarkus.maven-plugin.group-id"),
        QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID("quarkus.maven-plugin.artifact-id"),
        QUARKUS_MAVEN_PLUGIN_VERSION("quarkus.maven-plugin.version"),
        QUARKUS_GRADLE_PLUGIN_ID("quarkus.gradle-plugin.id"),
        QUARKUS_GRADLE_PLUGIN_VERSION("quarkus.gradle-plugin.version"),
        QUARKUS_VERSION("quarkus.version"),
        JAVA_VERSION("java.version"),

        RESTEASY_EXAMPLE_RESOURCE_PATH("resteasy-example.resource.path"),
        RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME("resteasy-example.resource.class-name"),

        SPRING_WEB_EXAMPLE_RESOURCE_PATH("spring-web-example.resource.path"),
        SPRING_WEB_EXAMPLE_RESOURCE_CLASS_NAME("spring-web-example.resource.class-name"),

        COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME("commandmode-example.main.class-name"),

        NO_EXAMPLES("quarkus-project.no-examples"),
        NO_BUILD_TOOL_WRAPPER("quarkus-project.no-build-tool-wrapper"),
        NO_DOCKERFILES("quarkus-project.no-dockerfiles");

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
        PROJECT_PACKAGE_NAME("package_name"),
        QUARKUS_MAVEN_PLUGIN_GROUP_ID("maven_plugin_groupId"),
        QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID("maven_plugin_artifactId"),
        QUARKUS_MAVEN_PLUGIN_VERSION("maven_plugin_version"),
        QUARKUS_GRADLE_PLUGIN_ID("gradle_plugin_id"),
        QUARKUS_GRADLE_PLUGIN_VERSION("gradle_plugin_version"),
        QUARKUS_VERSION("quarkus_version"),
        JAVA_VERSION("java_target"),

        RESTEASY_EXAMPLE_RESOURCE_PATH("path"),
        RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME(QuarkusCodestartData::convertClassName),

        SPRING_WEB_EXAMPLE_RESOURCE_PATH("path"),
        SPRING_WEB_EXAMPLE_RESOURCE_CLASS_NAME(QuarkusCodestartData::convertClassName);

        private final String key;
        private final Function<Map<String, Object>, Object> converter;

        LegacySupport(String legacyKey) {
            this((m) -> m.get(legacyKey));
        }

        LegacySupport(Function<Map<String, Object>, Object> converter) {
            this.key = DataKey.valueOf(this.name()).getKey();
            this.converter = converter;
        }

        public String getKey() {
            return key;
        }

        public static Map<String, Object> convertFromLegacy(Map<String, Object> legacy) {
            return NestedMaps.unflatten(Stream.of(values())
                    .map(v -> new HashMap.SimpleImmutableEntry<>(v.getKey(), v.converter.apply(legacy)))
                    .filter(v -> v.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    // TODO remove the class_name convertion when its removed
    private static String convertClassName(final Map<String, Object> legacyData) {
        Optional<String> classNameValue = NestedMaps.getValue(legacyData, "class_name");
        if (classNameValue.isPresent()) {
            final String className = classNameValue.get();
            int idx = classNameValue.get().lastIndexOf('.');
            if (idx < 0) {
                return className;
            }
            return className.substring(idx + 1);
        }
        return null;
    }
}
