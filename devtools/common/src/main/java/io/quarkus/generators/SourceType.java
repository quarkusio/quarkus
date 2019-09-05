package io.quarkus.generators;

import java.util.Arrays;
import java.util.Optional;

import io.quarkus.maven.utilities.MojoUtils;

public enum SourceType {
    JAVA(MojoUtils.JAVA_FILE_EXTENSION, MojoUtils.JAVA_EXTENSION_NAME),

    KOTLIN(MojoUtils.KOTLIN_FILE_EXTENSION, MojoUtils.KOTLIN_EXTENSION_NAME),

    SCALA(MojoUtils.SCALA_FILE_EXTENSION, MojoUtils.SCALA_EXTENSION_NAME);

    private static final String srcDirPrefix = "src/main/";
    private static final String testSrcDirPrefix = "src/test/";

    private static final String BUILD_FILE_RESOURCE_TEMPLATE = "templates/%s/%s/%s-template.ftl";
    private static final String RESOURCE_TEMPLATE = "templates/%s/%s/resource-template.ftl";
    private static final String SPRING_CONTROLLER_TEMPLATE = "templates/%s/%s/spring-controller-template.ftl";
    private static final String TEST_RESOURCE_TEMPLATE = "templates/%s/%s/test-resource-template.ftl";
    private static final String NATIVE_TEST_RESOURCE_TEMPLATE = "templates/%s/%s/native-test-resource-template.ftl";

    private final String extension;
    private final String name;

    SourceType(String extension, String name) {
        this.extension = extension;
        this.name = name;
    }

    public String getSrcDir() {
        return srcDirPrefix + getPathDiscriminator();
    }

    private String getPathDiscriminator() {
        return this.name;
    }

    public String getTestSrcDir() {
        return testSrcDirPrefix + getPathDiscriminator();
    }

    public String getBuildFileResourceTemplate(String templateName, String buildFile) {
        return computeTemplateFile(BUILD_FILE_RESOURCE_TEMPLATE, templateName, buildFile);
    }

    public String getSrcResourceTemplate(String templateName) {
        return computeTemplateFile(RESOURCE_TEMPLATE, templateName);
    }

    public String getSrcSpringControllerTemplate(String templateName) {
        return computeTemplateFile(SPRING_CONTROLLER_TEMPLATE, templateName);
    }

    public String getTestResourceTemplate(String templateName) {
        return computeTemplateFile(TEST_RESOURCE_TEMPLATE, templateName);
    }

    public String getNativeTestResourceTemplate(String templateName) {
        return computeTemplateFile(NATIVE_TEST_RESOURCE_TEMPLATE, templateName);
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    public String stripExtensionFrom(String className) {
        if (className != null && className.endsWith(extension)) {
            return className.substring(0, className.length() - extension.length());
        } else {
            return className;
        }
    }

    private String computeTemplateFile(String genericTemplate, String templateName) {
        return computeTemplateFile(genericTemplate, templateName, null);
    }

    private String computeTemplateFile(String genericTemplate, String templateName, String fileName) {
        return String.format(genericTemplate, templateName, getPathDiscriminator(), fileName);
    }

    public static Optional<SourceType> parse(String extension) {
        return Arrays.stream(values()).filter(e -> extension.contains(e.name)).findAny();
    }
}
