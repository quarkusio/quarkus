package io.quarkus.generators;

import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.devtools.writer.ProjectWriter;
import java.io.IOException;

public interface ProjectGenerator {
    String BOM_GROUP_ID = "bom_groupId";
    String BOM_ARTIFACT_ID = "bom_artifactId";
    String BOM_VERSION = "bom_version";
    String PROJECT_GROUP_ID = "project_groupId";
    String PROJECT_ARTIFACT_ID = "project_artifactId";
    String PROJECT_VERSION = "project_version";
    String QUARKUS_VERSION = "quarkus_version";
    String PACKAGE_NAME = "package_name";
    String SOURCE_TYPE = "source_type";
    String BUILD_FILE = "build_file";
    String BUILD_DIRECTORY = "build_dir";
    String ADDITIONAL_GITIGNORE_ENTRIES = "additional_gitignore_entries";
    String CLASS_NAME = "class_name";
    String IS_SPRING = "is_spring";
    String RESOURCE_PATH = "path";
    String JAVA_TARGET = "java_target";

    String getName();

    void generate(ProjectWriter writer, QuarkusCommandInvocation invocation) throws IOException;
}
