package io.quarkus.devtools.codestarts.quarkus;

import io.quarkus.devtools.codestarts.DataKey;

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
        PROJECT_NAME("project.name"),
        PROJECT_DESCRIPTION("project.description"),
        PROJECT_PACKAGE_NAME("project.package-name"),
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

        REST_CODESTART_RESOURCE_PATH("rest-codestart.resource.path"),
        REST_CODESTART_RESOURCE_CLASS_NAME("rest-codestart.resource.class-name"),

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

}
