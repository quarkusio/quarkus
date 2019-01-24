package org.jboss.shamrock;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class ShamrockTemplate {
    protected final Configuration cfg;

    /*
    Properties used in the templates.
    It cannot use `-` because of Freemarker interpreting `-` as minus.
     */

    public static final String PROJECT_GROUP_ID = "project_groupId";
    public static final String PROJECT_ARTIFACT_ID = "project_artifactId";
    public static final String PROJECT_VERSION = "project_version";
    public static final String SHAMROCK_VERSION = "shamrock_version";
    public static final String PACKAGE_NAME = "package_name";
    public static final String CLASS_NAME = "class_name";
    public static final String RESOURCE_PATH = "path";



    public ShamrockTemplate() {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setTemplateLoader(new ClassTemplateLoader(ShamrockTemplate.class, "/"));
    }

    public abstract String getName();

    public abstract void generate(final File projectRoot, Map<String, Object> parameters) throws IOException;
}
