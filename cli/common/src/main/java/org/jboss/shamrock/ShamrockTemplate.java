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

    public static String PROJECT_GROUP_ID = "project_groupId";
    public static String PROJECT_ARTIFACT_ID = "project_artifactId";
    public static String PROJECT_VERSION = "project_version";
    public static String SHAMROCK_VERSION = "shamrock_version";

    public ShamrockTemplate() {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setTemplateLoader(new ClassTemplateLoader(ShamrockTemplate.class, "/"));
    }

    public abstract String getName();

    public abstract void generate(final File projectRoot, Map<String, Object> parameters) throws IOException;
}
