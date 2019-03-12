package io.quarkus;

import io.quarkus.maven.utilities.MojoUtils;

public enum SourceType {
    JAVA(MojoUtils.JAVA_EXTENSION,
            "src/main/java",
            "src/test/java",
            "templates/%s/pom-template.ftl",
            "templates/%s/resource-template.ftl",
            "templates/%s/test-resource-template.ftl",
            "templates/%s/native-test-resource-template.ftl"),

    KOTLIN(MojoUtils.KOTLIN_EXTENSION,
            "src/main/kotlin",
            "src/test/kotlin",
            "templates/%s/pom-template-kotlin.ftl",
            "templates/%s/resource-template-kotlin.ftl",
            "templates/%s/test-resource-template-kotlin.ftl",
            "templates/%s/native-test-resource-template-kotlin.ftl");

    private final String extension;
    private final String srcDir;
    private final String testSrcDir;
    private final String pomResourceTemplate;
    private final String srcResourceTemplate;
    private final String testResourceTemplate;
    private final String nativeTestResourceTemplate;

    SourceType(String extension, String srcDir, String testSrcDir, String pomResourceTemplate, String srcResourceTemplate,
            String testResourceTemplate,
            String nativeTestResourceTemplate) {
        this.extension = extension;
        this.srcDir = srcDir;
        this.testSrcDir = testSrcDir;
        this.pomResourceTemplate = pomResourceTemplate;
        this.srcResourceTemplate = srcResourceTemplate;
        this.testResourceTemplate = testResourceTemplate;
        this.nativeTestResourceTemplate = nativeTestResourceTemplate;
    }

    public String getSrcDir() {
        return srcDir;
    }

    public String getTestSrcDir() {
        return testSrcDir;
    }

    public String getPomResourceTemplate(String templateName) {
        return computeTemplateFile(pomResourceTemplate, templateName);
    }

    public String getSrcResourceTemplate(String templateName) {
        return computeTemplateFile(srcResourceTemplate, templateName);
    }

    public String getTestResourceTemplate(String templateName) {
        return computeTemplateFile(testResourceTemplate, templateName);
    }

    public String getNativeTestResourceTemplate(String templateName) {
        return computeTemplateFile(nativeTestResourceTemplate, templateName);
    }

    public String getExtension() {
        return extension;
    }

    public String stripExtensionFrom(String className) {
        if (className.contains(extension)) {
            return className.substring(0, className.length() - extension.length());
        } else {
            return className;
        }
    }

    private String computeTemplateFile(String genericTemplate, String templateName) {
        return String.format(genericTemplate, templateName);
    }
}
