package io.quarkus.generators;

import java.io.IOException;
import java.util.Map;

import io.quarkus.cli.commands.writer.ProjectWriter;

public interface ProjectGenerator {
    String PROJECT_GROUP_ID = "project_groupId";
    String PROJECT_ARTIFACT_ID = "project_artifactId";
    String PROJECT_VERSION = "project_version";
    String QUARKUS_VERSION = "quarkus_version";
    String PACKAGE_NAME = "package_name";
    String SOURCE_TYPE = "source_type";
    String BUILD_TOOL = "build_tool";
    String ADDITIONAL_GITIGNORE_ENTRIES = "additional_gitignore_entries";
    String CLASS_NAME = "class_name";
    String RESOURCE_PATH = "path";

    String getName();

    void generate(final ProjectWriter writer, Map<String, Object> parameters) throws IOException;
}
