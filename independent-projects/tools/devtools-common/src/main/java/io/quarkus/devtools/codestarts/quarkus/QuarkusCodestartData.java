package io.quarkus.devtools.codestarts.quarkus;

import io.quarkus.devtools.codestarts.DataKey;
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

    public enum QuarkusDataKey implements DataKey {
        BOM_GROUP_ID("quarkus.platform.group-id"),
        BOM_ARTIFACT_ID("quarkus.platform.artifact-id"),
        BOM_VERSION("quarkus.platform.version"),
        PROJECT_GROUP_ID("project.group-id"),
        PROJECT_ARTIFACT_ID("project.artifact-id"),
        PROJECT_VERSION("project.version"),
        PROJECT_PACKAGE_NAME("project.package-name"),
        PROJECT_PARENT_GROUP_ID("parent.group-id"),
        PROJECT_PARENT_ARTIFACT_ID("parent.artifact-id"),
        PROJECT_PARENT_VERSION("parent.version"),
        PROJECT_PARENT_PATH("parent.path"),
        QUARKUS_MAVEN_PLUGIN_GROUP_ID("quarkus.maven-plugin.group-id"),
        QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID("quarkus.maven-plugin.artifact-id"),
        QUARKUS_MAVEN_PLUGIN_VERSION("quarkus.maven-plugin.version"),
        QUARKUS_GRADLE_PLUGIN_ID("quarkus.gradle-plugin.id"),
        QUARKUS_GRADLE_PLUGIN_VERSION("quarkus.gradle-plugin.version"),
        QUARKUS_VERSION("quarkus.version"),

        JAVA_VERSION("java.version"),
        KOTLIN_VERSION("kotlin.version"),
        SCALA_VERSION("scala.version"),
        SCALA_MAVEN_PLUGIN_VERSION("scala-maven-plugin.version"),
        MAVEN_COMPILER_PLUGIN_VERSION("maven-compiler-plugin.version"),
        MAVEN_SUREFIRE_PLUGIN_VERSION("maven-surefire-plugin.version"),

        RESTEASY_CODESTART_RESOURCE_PATH("resteasy-codestart.resource.path"),
        RESTEASY_CODESTART_RESOURCE_CLASS_NAME("resteasy-codestart.resource.class-name"),

        RESTEASY_REACTIVE_CODESTART_RESOURCE_PATH("resteasy-reactive-codestart.resource.path"),
        RESTEASY_REACTIVE_CODESTART_RESOURCE_CLASS_NAME("resteasy-reactive-codestart.resource.class-name"),

        SPRING_WEB_CODESTART_RESOURCE_PATH("spring-web-codestart.resource.path"),
        SPRING_WEB_CODESTART_RESOURCE_CLASS_NAME("spring-web-codestart.resource.class-name"),

        APP_CONFIG("app-config");

        private final String key;

        QuarkusDataKey(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    public enum LegacySupport implements DataKey {
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
        KOTLIN_VERSION("kotlin_version"),
        SCALA_VERSION("scala_version"),
        SCALA_MAVEN_PLUGIN_VERSION("scala_plugin_version"),
        MAVEN_COMPILER_PLUGIN_VERSION("compiler_plugin_version"),
        MAVEN_SUREFIRE_PLUGIN_VERSION("surefire_plugin_version"),

        RESTEASY_CODESTART_RESOURCE_PATH("path"),
        RESTEASY_CODESTART_RESOURCE_CLASS_NAME(QuarkusCodestartData::convertClassName),

        RESTEASY_REACTIVE_CODESTART_RESOURCE_PATH("path"),
        RESTEASY_REACTIVE_CODESTART_RESOURCE_CLASS_NAME(QuarkusCodestartData::convertClassName),

        SPRING_WEB_CODESTART_RESOURCE_PATH("path"),
        SPRING_WEB_CODESTART_RESOURCE_CLASS_NAME(QuarkusCodestartData::convertClassName);

        private final String key;
        private final Function<Map<String, Object>, Object> converter;

        LegacySupport(String legacyKey) {
            this((m) -> m.get(legacyKey));
        }

        LegacySupport(Function<Map<String, Object>, Object> converter) {
            this.key = QuarkusDataKey.valueOf(this.name()).key();
            this.converter = converter;
        }

        @Override
        public String key() {
            return key;
        }

        public static Map<String, Object> convertFromLegacy(Map<String, Object> legacy) {
            return NestedMaps.unflatten(Stream.of(values())
                    .map(v -> new HashMap.SimpleImmutableEntry<>(v.key(), v.converter.apply(legacy)))
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
