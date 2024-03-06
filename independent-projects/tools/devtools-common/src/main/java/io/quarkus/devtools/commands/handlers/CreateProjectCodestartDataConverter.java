package io.quarkus.devtools.commands.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;

public enum CreateProjectCodestartDataConverter implements DataKey {
    PROJECT_GROUP_ID(CreateProjectKey.PROJECT_GROUP_ID),
    PROJECT_ARTIFACT_ID(CreateProjectKey.PROJECT_ARTIFACT_ID),
    PROJECT_VERSION(CreateProjectKey.PROJECT_VERSION),
    PROJECT_NAME(CreateProjectKey.PROJECT_NAME),
    PROJECT_DESCRIPTION(CreateProjectKey.PROJECT_DESCRIPTION),
    PROJECT_PACKAGE_NAME(CreateProjectKey.PACKAGE_NAME),
    QUARKUS_VERSION(CreateProjectKey.QUARKUS_VERSION),
    JAVA_VERSION(CreateProjectKey.JAVA_VERSION),
    APP_CONFIG(CreateProjectKey.APP_CONFIG, Collections.emptyMap()),

    QUARKUS_MAVEN_PLUGIN_GROUP_ID(PlatformPropertiesKey.QUARKUS_MAVEN_PLUGIN_GROUP_ID),
    QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID(PlatformPropertiesKey.QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID),
    QUARKUS_MAVEN_PLUGIN_VERSION(PlatformPropertiesKey.QUARKUS_MAVEN_PLUGIN_VERSION),
    QUARKUS_GRADLE_PLUGIN_ID(PlatformPropertiesKey.QUARKUS_GRADLE_PLUGIN_ID),
    QUARKUS_GRADLE_PLUGIN_VERSION(PlatformPropertiesKey.QUARKUS_GRADLE_PLUGIN_VERSION),
    KOTLIN_VERSION(PlatformPropertiesKey.KOTLIN_VERSION),
    SCALA_VERSION(PlatformPropertiesKey.SCALA_VERSION),
    SCALA_MAVEN_PLUGIN_VERSION(PlatformPropertiesKey.SCALA_MAVEN_PLUGIN_VERSION),
    MAVEN_COMPILER_PLUGIN_VERSION(PlatformPropertiesKey.MAVEN_COMPILER_PLUGIN_VERSION),
    MAVEN_SUREFIRE_PLUGIN_VERSION(PlatformPropertiesKey.MAVEN_SUREFIRE_PLUGIN_VERSION),

    BOM_GROUP_ID(CatalogKey.BOM_GROUP_ID),
    BOM_ARTIFACT_ID(CatalogKey.BOM_ARTIFACT_ID),
    BOM_VERSION(CatalogKey.BOM_VERSION),

    RESTEASY_CODESTART_RESOURCE_PATH(CreateProjectKey.RESOURCE_PATH),
    RESTEASY_CODESTART_RESOURCE_CLASS_NAME(CreateProjectCodestartDataConverter::convertClassName),

    REST_CODESTART_RESOURCE_PATH(CreateProjectKey.RESOURCE_PATH),
    REST_CODESTART_RESOURCE_CLASS_NAME(CreateProjectCodestartDataConverter::convertClassName),

    SPRING_WEB_CODESTART_RESOURCE_PATH(CreateProjectKey.RESOURCE_PATH),
    SPRING_WEB_CODESTART_RESOURCE_CLASS_NAME(CreateProjectCodestartDataConverter::convertClassName);

    private final String key;
    private final Function<Map<String, Object>, Object> converter;

    CreateProjectCodestartDataConverter(String createProjectKey) {
        this((m) -> m.get(createProjectKey));
    }

    CreateProjectCodestartDataConverter(String createProjectKey, Object defaultValue) {
        this((m) -> m.getOrDefault(createProjectKey, defaultValue));
    }

    CreateProjectCodestartDataConverter(Function<Map<String, Object>, Object> converter) {
        this.key = QuarkusCodestartData.QuarkusDataKey.valueOf(this.name()).key();
        this.converter = converter;
    }

    @Override
    public String key() {
        return key;
    }

    public static Map<String, Object> toCodestartData(Map<String, Object> createProjectData) {
        return NestedMaps.unflatten(Stream.of(values())
                .map(v -> new HashMap.SimpleImmutableEntry<>(v.key(), v.converter.apply(createProjectData)))
                .filter(v -> v.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private static String convertClassName(final Map<String, Object> createProjectData) {
        String className = (String) createProjectData.get(CreateProjectKey.RESOURCE_CLASS_NAME);
        if (className != null) {
            int idx = className.lastIndexOf('.');
            if (idx < 0) {
                return className;
            }
            return className.substring(idx + 1);
        }
        return null;
    }

    public interface PlatformPropertiesKey {
        String QUARKUS_MAVEN_PLUGIN_GROUP_ID = "maven-plugin-groupId";
        String QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID = "maven-plugin-artifactId";
        String QUARKUS_MAVEN_PLUGIN_VERSION = "maven-plugin-version";
        String QUARKUS_GRADLE_PLUGIN_VERSION = "gradle-plugin-version";
        String KOTLIN_VERSION = "kotlin-version";
        String SCALA_VERSION = "scala-version";
        String QUARKUS_GRADLE_PLUGIN_ID = "gradle-plugin-id";
        String SCALA_MAVEN_PLUGIN_VERSION = "scala-plugin-version";
        String MAVEN_COMPILER_PLUGIN_VERSION = "compiler-plugin-version";
        String MAVEN_SUREFIRE_PLUGIN_VERSION = "surefire-plugin-version";
    }

    public interface CatalogKey {
        String BOM_GROUP_ID = "bom.group-id";
        String BOM_ARTIFACT_ID = "bom.artifact-id";
        String BOM_VERSION = "bom.version";
    }

}
