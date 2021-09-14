package io.quarkus.devtools.project.codegen;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import java.io.IOException;

public interface ProjectGenerator {
    String BOM_GROUP_ID = "bom_groupId";
    String BOM_ARTIFACT_ID = "bom_artifactId";
    String BOM_VERSION = "bom_version";
    String PROJECT_GROUP_ID = "project_groupId";
    String PROJECT_ARTIFACT_ID = "project_artifactId";
    String PROJECT_VERSION = "project_version";
    String QUARKUS_MAVEN_PLUGIN_VERSION = "maven_plugin_version";
    String QUARKUS_GRADLE_PLUGIN_VERSION = "gradle_plugin_version";
    String QUARKUS_VERSION = "quarkus_version";
    String PACKAGE_NAME = "package_name";
    String MAVEN_REPOSITORIES = "maven_repositories";
    String MAVEN_PLUGIN_REPOSITORIES = "maven_plugin_repositories";
    String SOURCE_TYPE = "source_type";
    String BUILD_FILE = "build_file";
    String BUILD_DIRECTORY = "build_dir";
    String ADDITIONAL_GITIGNORE_ENTRIES = "additional_gitignore_entries";
    String CLASS_NAME = "class_name";
    String EXTENSIONS = "extensions";
    String IS_SPRING = "is_spring";
    String RESOURCE_PATH = "path";
    String JAVA_TARGET = "java_target";
    String APP_CONFIG = "app-config"; // codestart uses dashes

    String getName();

    void generate(QuarkusCommandInvocation invocation) throws IOException;
}
