package io.quarkus;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface QuarkusTemplate {
    String PROJECT_GROUP_ID = "project_groupId";
    String PROJECT_ARTIFACT_ID = "project_artifactId";
    String PROJECT_VERSION = "project_version";
    String QUARKUS_VERSION = "quarkus_version";
    String PACKAGE_NAME = "package_name";
    String SOURCE_TYPE = "source_type";
    String CLASS_NAME = "class_name";
    String RESOURCE_PATH = "path";

    String getName();

    void generate(final File projectRoot, Map<String, Object> parameters) throws IOException;
}
