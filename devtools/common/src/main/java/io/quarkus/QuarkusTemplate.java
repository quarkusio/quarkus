package io.quarkus;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class QuarkusTemplate {
    public static final String PROJECT_GROUP_ID = "project_groupId";
    public static final String PROJECT_ARTIFACT_ID = "project_artifactId";
    public static final String PROJECT_VERSION = "project_version";
    public static final String QUARKUS_VERSION = "quarkus_version";
    public static final String PACKAGE_NAME = "package_name";
    public static final String SOURCE_TYPE = "source_type";
    public static final String CLASS_NAME = "class_name";
    public static final String RESOURCE_PATH = "path";

    private static final Map<String, QuarkusTemplate> templates = new ConcurrentHashMap<>(7);

    public static QuarkusTemplate createTemplateWith(String name) throws IllegalArgumentException {
        final QuarkusTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Unknown template: " + name);
        }

        return template;
    }

    protected static boolean registerTemplate(QuarkusTemplate template) {
        if (template == null) {
            return false;
        }

        return templates.put(template.getName(), template) == null;
    }

    public abstract String getName();

    public abstract void generate(final File projectRoot, Map<String, Object> parameters) throws IOException;
}
