package io.quarkus.platform.tools;

public interface ToolsConstants {

    String IO_QUARKUS = "io.quarkus";

    String QUARKUS = "quarkus";
    String QUARKUS_CORE_GROUP_ID = IO_QUARKUS;
    String QUARKUS_CORE_ARTIFACT_ID = "quarkus-core";

    String QUARKUS_MAVEN_PLUGIN = "quarkus-maven-plugin";

    String DEFAULT_PLATFORM_BOM_GROUP_ID = IO_QUARKUS;
    String DEFAULT_PLATFORM_BOM_ARTIFACT_ID = "quarkus-universe-bom";

    String PROP_QUARKUS_CORE_VERSION = "quarkus-core-version";

    String PROP_QUARKUS_MAVEN_PLUGIN_GROUP_ID = "maven-plugin-groupId";
    String PROP_QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID = "maven-plugin-artifactId";
    String PROP_QUARKUS_MAVEN_PLUGIN_VERSION = "maven-plugin-version";

    String PROP_QUARKUS_GRADLE_PLUGIN_ID = "gradle-plugin-id";
    String PROP_QUARKUS_GRADLE_PLUGIN_VERSION = "gradle-plugin-version";

    String PROP_PROPOSED_MVN_VERSION = "proposed-maven-version";
    String PROP_MVN_WRAPPER_VERSION = "maven-wrapper-version";
    String PROP_GRADLE_WRAPPER_VERSION = "gradle-wrapper-version";
}
