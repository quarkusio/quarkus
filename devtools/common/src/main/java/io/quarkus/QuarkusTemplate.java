package io.quarkus;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class QuarkusTemplate {
    public static final String PROJECT_GROUP_ID = "project_groupId";
    public static final String PROJECT_ARTIFACT_ID = "project_artifactId";
    public static final String PROJECT_VERSION = "project_version";
    public static final String QUARKUS_VERSION = "quarkus_version";
    public static final String PACKAGE_NAME = "package_name";
    public static final String CLASS_NAME = "class_name";
    public static final String RESOURCE_PATH = "path";

    public abstract String getName();

    public abstract void generate(final File projectRoot, Map<String, Object> parameters) throws IOException;
}
